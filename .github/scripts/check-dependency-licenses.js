#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const https = require('https');

// Lizenz-Kategorien
const OK_LICENSES = [
  'Apache-2.0', 'Apache 2.0', 'Apache License 2.0',
  'MIT', 'MIT License',
  'BSD', 'BSD-2-Clause', 'BSD-3-Clause', 'BSD-4-Clause',
  'LGPL', 'LGPL-2.1', 'LGPL-3.0', 'Lesser General Public License',
  'GPL', 'GPL-2.0', 'GPL-3.0', 'GNU General Public License',
  'EPL', 'EPL-1.0', 'EPL-2.0', 'Eclipse Public License',
  'CDDL', 'CDDL-1.0', 'CDDL-1.1',
  'MPL', 'MPL-1.1', 'MPL-2.0', 'Mozilla Public License',
  'ISC',
  'Public Domain',
  'CC0', 'CC0-1.0',
  'Unlicense'
];

// Problematische Lizenzen (kommerzielle Nutzung verboten oder nur gegen Gebühren)
const PROBLEMATIC_LICENSES = [
  'Commercial', 'Proprietary', 'Commercial License',
  'Trial', 'Evaluation', 'Demo'
];

// Lizenzen, die kommerzielle Nutzung einschränken (z.B. AGPL in manchen Kontexten)
const RESTRICTIVE_LICENSES = [
  'AGPL', 'AGPL-3.0', 'Affero General Public License'
];

// Bekannte kommerzielle Vaadin Addons (Pattern-Matching)
const COMMERCIAL_VAADIN_ADDONS = [
  /vaadin.*pro/i,
  /vaadin.*enterprise/i,
  /vaadin.*commercial/i,
  /vaadin.*premium/i
];

const results = {
  errors: [],
  warnings: [],
  checked: 0,
  skipped: 0
};

// Hilfsfunktion: HTTP GET Request
function httpsGet(url) {
  return new Promise((resolve, reject) => {
    https.get(url, (res) => {
      let data = '';
      res.on('data', (chunk) => { data += chunk; });
      res.on('end', () => {
        if (res.statusCode === 200) {
          try {
            resolve(JSON.parse(data));
          } catch (e) {
            resolve(null);
          }
        } else {
          resolve(null);
        }
      });
    }).on('error', (err) => {
      reject(err);
    });
  });
}

// Parse build.gradle und extrahiere Dependencies
function parseBuildGradle(buildGradlePath) {
  const content = fs.readFileSync(buildGradlePath, 'utf8');
  const dependencies = [];
  
  // Extrahiere Variablen aus ext {} Block
  const extVars = {};
  const extMatch = content.match(/ext\s*\{([^}]+)\}/s);
  if (extMatch) {
    const extContent = extMatch[1];
    const varRegex = /(\w+)\s*=\s*['"]([^'"]+)['"]/g;
    let varMatch;
    while ((varMatch = varRegex.exec(extContent)) !== null) {
      extVars[varMatch[1]] = varMatch[2];
    }
  }
  
  // Regex für Dependencies (implementation, testImplementation, etc.)
  // Unterstützt auch String-Interpolation mit ${variable}
  const dependencyRegex = /(?:implementation|testImplementation|runtimeOnly|compileOnly|annotationProcessor|developmentOnly)\s+['"]([^'"]+)['"]/g;
  
  let match;
  while ((match = dependencyRegex.exec(content)) !== null) {
    let depString = match[1];
    
    // Ersetze Variablen-Referenzen (z.B. ${mapstructVersion})
    depString = depString.replace(/\$\{(\w+)\}/g, (match, varName) => {
      return extVars[varName] || match;
    });
    
    // Parse group:artifact:version
    const parts = depString.split(':');
    if (parts.length >= 2) {
      const groupId = parts[0];
      const artifactId = parts[1];
      // Version kann fehlen (wird über BOM verwaltet) oder leer sein
      const version = parts.length >= 3 && parts[2] ? parts[2] : null;
      
      dependencies.push({ groupId, artifactId, version, full: depString });
    }
  }
  
  return dependencies;
}

// Prüfe Lizenz über Maven Central API
async function checkLicense(groupId, artifactId, version) {
  try {
    // Wenn Version null, 'latest' oder leer, hole die neueste Version
    let targetVersion = version;
    
    if (!targetVersion || targetVersion === 'latest' || targetVersion.includes('${')) {
      // Maven Central Search API für neueste Version
      const searchUrl = `https://search.maven.org/solrsearch/select?q=g:${encodeURIComponent(groupId)}+AND+a:${encodeURIComponent(artifactId)}&rows=1&wt=json`;
      
      const searchResult = await httpsGet(searchUrl);
      if (!searchResult || !searchResult.response || !searchResult.response.docs || searchResult.response.docs.length === 0) {
        return { found: false, licenses: [] };
      }
      
      const doc = searchResult.response.docs[0];
      targetVersion = doc.latestVersion || doc.v;
    }
    
    // Hole POM für Lizenz-Informationen
    return await fetchLicenseFromPom(groupId, artifactId, targetVersion);
    
  } catch (error) {
    console.warn(`⚠️  Fehler beim Abrufen der Lizenz für ${groupId}:${artifactId}: ${error.message}`);
    return { found: false, licenses: [], error: error.message };
  }
}

// Hole Lizenz-Informationen aus POM
async function fetchLicenseFromPom(groupId, artifactId, version) {
  try {
    const pomPath = `${groupId.replace(/\./g, '/')}/${artifactId}/${version}`;
    const pomUrl = `https://repo1.maven.org/maven2/${pomPath}/${artifactId}-${version}.pom`;
    
    return new Promise((resolve) => {
      https.get(pomUrl, (res) => {
        let data = '';
        res.on('data', (chunk) => { data += chunk; });
        res.on('end', () => {
          if (res.statusCode === 200) {
            // Parse POM XML (vereinfacht)
            const licenses = [];
            
            // Suche nach <license> Tags
            const licenseRegex = /<license>[\s\S]*?<\/license>/gi;
            const matches = data.match(licenseRegex);
            
            if (matches) {
              for (const match of matches) {
                const nameMatch = match.match(/<name>([^<]+)<\/name>/i);
                const urlMatch = match.match(/<url>([^<]+)<\/url>/i);
                
                if (nameMatch) {
                  licenses.push({
                    name: nameMatch[1].trim(),
                    url: urlMatch ? urlMatch[1].trim() : null
                  });
                }
              }
            }
            
            resolve({ found: true, licenses, version });
          } else {
            resolve({ found: false, licenses: [] });
          }
        });
      }).on('error', () => {
        resolve({ found: false, licenses: [] });
      });
    });
  } catch (error) {
    return { found: false, licenses: [] };
  }
}

// Prüfe ob Lizenz problematisch ist
function isProblematicLicense(licenseName, groupId, artifactId) {
  if (!licenseName) return { problematic: false, reason: null };
  
  const licenseLower = licenseName.toLowerCase();
  
  // Prüfe auf bekannte problematische Lizenzen
  for (const problematic of PROBLEMATIC_LICENSES) {
    if (licenseLower.includes(problematic.toLowerCase())) {
      return { problematic: true, reason: `Kommerzielle Lizenz erforderlich: ${licenseName}` };
    }
  }
  
  // Prüfe auf bekannte OK-Lizenzen
  for (const okLicense of OK_LICENSES) {
    if (licenseLower.includes(okLicense.toLowerCase())) {
      return { problematic: false, reason: null };
    }
  }
  
  // Prüfe auf Vaadin kommerzielle Addons
  const fullName = `${groupId}:${artifactId}`.toLowerCase();
  for (const pattern of COMMERCIAL_VAADIN_ADDONS) {
    if (pattern.test(fullName)) {
      return { problematic: true, reason: `Kommerzielles Vaadin Addon erkannt: ${groupId}:${artifactId}` };
    }
  }
  
  // Unbekannte Lizenz - Warnung
  return { problematic: false, reason: null, unknown: true };
}

// Hauptfunktion
async function main() {
  console.log('🔍 Prüfe Dependencies in build.gradle auf problematische Lizenzen...\n');
  
  const buildGradlePath = path.join(process.cwd(), 'build.gradle');
  
  if (!fs.existsSync(buildGradlePath)) {
    console.error('❌ build.gradle nicht gefunden!');
    process.exit(1);
  }
  
  const dependencies = parseBuildGradle(buildGradlePath);
  console.log(`📦 ${dependencies.length} Dependencies gefunden\n`);
  
  // Prüfe jede Dependency
  for (const dep of dependencies) {
    results.checked++;
    
    // Skip bekannte OK-Dependencies (z.B. Spring Boot, Vaadin Core ohne Pro/Enterprise)
    // Aber prüfe Vaadin Addons explizit, da es kommerzielle gibt
    if (dep.groupId.startsWith('org.springframework')) {
      results.skipped++;
      continue;
    }
    
    // Vaadin Core ist OK, aber Addons müssen geprüft werden
    if (dep.groupId === 'com.vaadin' && 
        (dep.artifactId === 'vaadin' || dep.artifactId === 'vaadin-spring-boot-starter' || dep.artifactId === 'control-center-starter')) {
      results.skipped++;
      continue;
    }
    
    console.log(`🔍 Prüfe ${dep.groupId}:${dep.artifactId}...`);
    
    try {
      const licenseInfo = await checkLicense(dep.groupId, dep.artifactId, dep.version);
      
      if (!licenseInfo.found || licenseInfo.licenses.length === 0) {
        // Keine Lizenz-Info gefunden - Warnung
        results.warnings.push({
          dependency: `${dep.groupId}:${dep.artifactId}:${dep.version}`,
          reason: 'Lizenz-Informationen nicht gefunden - bitte manuell prüfen'
        });
        continue;
      }
      
      // Prüfe alle gefundenen Lizenzen
      let hasProblematic = false;
      let hasUnknown = false;
      
      for (const license of licenseInfo.licenses) {
        const check = isProblematicLicense(license.name, dep.groupId, dep.artifactId);
        
        if (check.problematic) {
          hasProblematic = true;
          results.errors.push({
            dependency: `${dep.groupId}:${dep.artifactId}:${dep.version}`,
            license: license.name,
            licenseUrl: license.url,
            reason: check.reason
          });
        } else if (check.unknown) {
          hasUnknown = true;
        }
      }
      
      if (!hasProblematic && hasUnknown) {
        // Unbekannte Lizenz - Warnung
        const licenseNames = licenseInfo.licenses.map(l => l.name).join(', ');
        results.warnings.push({
          dependency: `${dep.groupId}:${dep.artifactId}:${dep.version}`,
          license: licenseNames,
          reason: 'Unbekannte Lizenz - bitte manuell prüfen ob kommerzielle Nutzung erlaubt ist'
        });
      }
      
      // Rate limiting - kleine Pause zwischen Requests
      await new Promise(resolve => setTimeout(resolve, 200));
      
    } catch (error) {
      results.warnings.push({
        dependency: `${dep.groupId}:${dep.artifactId}:${dep.version}`,
        reason: `Fehler beim Prüfen: ${error.message}`
      });
    }
  }
  
  // Generiere Zusammenfassung
  const summaryPath = path.join(process.cwd(), '.github', 'license-check-results.md');
  let summary = '## 📋 Dependency-Lizenzprüfung - Zusammenfassung\n\n';
  
  summary += `- ✅ ${results.checked} Dependencies geprüft\n`;
  summary += `- ⏭️  ${results.skipped} bekannte OK-Dependencies übersprungen\n\n`;
  
  if (results.errors.length === 0 && results.warnings.length === 0) {
    summary += '✅ **Keine problematischen Lizenzen gefunden**\n\n';
    summary += 'Alle geprüften Dependencies verwenden freie Lizenzen (Apache 2.0, MIT, GPL, etc.), die kommerzielle Nutzung erlauben.\n\n';
    console.log('\n✅ Keine problematischen Lizenzen gefunden');
  } else {
    if (results.errors.length > 0) {
      summary += '## ❌ Problematische Lizenzen gefunden:\n\n';
      console.log(`\n❌ ${results.errors.length} problematische Lizenzen gefunden:`);
      
      for (const error of results.errors) {
        summary += `### ${error.dependency}\n\n`;
        summary += `- **Lizenz:** ${error.license}\n`;
        if (error.licenseUrl) {
          summary += `- **Lizenz-URL:** ${error.licenseUrl}\n`;
        }
        summary += `- **Grund:** ${error.reason}\n\n`;
        
        console.log(`  ❌ ${error.dependency}: ${error.license} - ${error.reason}`);
      }
      summary += '\n';
    }
    
    if (results.warnings.length > 0) {
      summary += '## ⚠️  Warnungen:\n\n';
      console.log(`\n⚠️  ${results.warnings.length} Warnungen:`);
      
      for (const warning of results.warnings) {
        summary += `### ${warning.dependency}\n\n`;
        if (warning.license) {
          summary += `- **Lizenz:** ${warning.license}\n`;
        }
        summary += `- **Grund:** ${warning.reason}\n\n`;
        
        console.log(`  ⚠️  ${warning.dependency}: ${warning.reason}`);
      }
      summary += '\n';
    }
    
    summary += '## 📝 Empfehlungen:\n\n';
    summary += '- ✅ Prüfe die gefundenen Lizenzen auf kommerzielle Nutzungsrechte\n';
    summary += '- ✅ Ersetze Dependencies mit problematischen Lizenzen durch Alternativen mit freien Lizenzen\n';
    summary += '- ✅ Dokumentiere alle verwendeten Software-Lizenzen\n\n';
  }
  
  // Schreibe Zusammenfassung
  const summaryDir = path.dirname(summaryPath);
  if (!fs.existsSync(summaryDir)) {
    fs.mkdirSync(summaryDir, { recursive: true });
  }
  fs.writeFileSync(summaryPath, summary);
  
  // Schreibe auch in GitHub Step Summary
  if (process.env.GITHUB_STEP_SUMMARY) {
    fs.appendFileSync(process.env.GITHUB_STEP_SUMMARY, summary);
  }
  
  // Exit-Code basierend auf Fehlern
  if (results.errors.length > 0) {
    console.error('\n❌ Problematische Lizenzen gefunden!');
    process.exit(1);
  }
  
  console.log('\n✅ Lizenzprüfung erfolgreich abgeschlossen');
  process.exit(0);
}

// Führe Script aus
main().catch((error) => {
  console.error('❌ Fehler beim Ausführen des Scripts:', error);
  process.exit(1);
});

