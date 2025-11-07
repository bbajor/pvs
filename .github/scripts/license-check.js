#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// Lade Konfiguration
const configPath = path.join(__dirname, '..', 'license-check-config.json');
const config = JSON.parse(fs.readFileSync(configPath, 'utf8'));

const results = {
  errors: [],
  warnings: [],
  checkedFiles: 0,
  checkedLines: 0
};

// Hilfsfunktion: Prüfe ob Datei ausgeschlossen werden soll
function isExcluded(filePath) {
  const relativePath = path.relative(process.cwd(), filePath);
  
  // Prüfe ausgeschlossene Pfade
  for (const excludedPath of config.excludedPaths) {
    if (relativePath.includes(excludedPath)) {
      return true;
    }
  }
  
  // Prüfe ausgeschlossene Dateierweiterungen
  const ext = path.extname(filePath);
  if (config.excludedExtensions.includes(ext)) {
    return true;
  }
  
  return false;
}

// Hilfsfunktion: Suche nach Pattern in Dateien
function searchInFile(filePath, patterns) {
  try {
    const content = fs.readFileSync(filePath, 'utf8');
    const lines = content.split('\n');
    const matches = [];
    
    for (let i = 0; i < lines.length; i++) {
      const line = lines[i];
      
      for (const pattern of patterns) {
        const regex = new RegExp(pattern.pattern, 'gi');
        if (regex.test(line)) {
          matches.push({
            pattern: pattern.name,
            line: i + 1,
            content: line.trim(),
            severity: pattern.severity,
            reason: pattern.reason,
            alternative: pattern.alternative
          });
        }
      }
    }
    
    return matches;
  } catch (error) {
    // Ignoriere Fehler beim Lesen von Binärdateien
    return [];
  }
}

// Hauptfunktion: Durchsuche Verzeichnis rekursiv
function scanDirectory(dirPath) {
  const entries = fs.readdirSync(dirPath, { withFileTypes: true });
  
  for (const entry of entries) {
    const fullPath = path.join(dirPath, entry.name);
    
    if (isExcluded(fullPath)) {
      continue;
    }
    
    if (entry.isDirectory()) {
      scanDirectory(fullPath);
    } else if (entry.isFile()) {
      results.checkedFiles++;
      
      // Prüfe auf problematische Software
      const problematicMatches = searchInFile(fullPath, config.problematicSoftware);
      for (const match of problematicMatches) {
        results.errors.push({
          file: path.relative(process.cwd(), fullPath),
          ...match
        });
      }
      
      // Prüfe auf Warnungen
      const warningMatches = searchInFile(fullPath, config.warnings);
      for (const match of warningMatches) {
        results.warnings.push({
          file: path.relative(process.cwd(), fullPath),
          ...match
        });
      }
    }
  }
}

// Hauptausführung
console.log('🔍 Prüfe Code auf lizenzrechtlich problematische Software...\n');

// Starte Suche
const rootDir = process.cwd();
scanDirectory(rootDir);

// Generiere Zusammenfassung
const summaryPath = path.join(process.cwd(), '.github', 'license-check-results.md');
let summary = '## 📋 Lizenzprüfung - Zusammenfassung\n\n';

summary += `- ✅ ${results.checkedFiles} Dateien geprüft\n\n`;

if (results.errors.length === 0 && results.warnings.length === 0) {
  summary += '✅ **Keine lizenzrechtlich problematischen Referenzen gefunden**\n\n';
  console.log('✅ Keine lizenzrechtlich problematischen Referenzen gefunden');
} else {
  if (results.errors.length > 0) {
    summary += '## ❌ Fehler gefunden:\n\n';
    console.log(`❌ ${results.errors.length} Fehler gefunden:`);
    
    for (const error of results.errors) {
      summary += `### ${error.pattern}\n\n`;
      summary += `- **Datei:** \`${error.file}:${error.line}\`\n`;
      summary += `- **Grund:** ${error.reason}\n`;
      summary += `- **Alternative:** ${error.alternative}\n`;
      summary += `- **Code:** \`${error.content.substring(0, 100)}${error.content.length > 100 ? '...' : ''}\`\n\n`;
      
      console.log(`  ❌ ${error.pattern} in ${error.file}:${error.line}`);
      console.log(`     Grund: ${error.reason}`);
      console.log(`     Alternative: ${error.alternative}`);
    }
    summary += '\n';
  }
  
  if (results.warnings.length > 0) {
    summary += '## ⚠️  Warnungen:\n\n';
    console.log(`\n⚠️  ${results.warnings.length} Warnungen:`);
    
    // Gruppiere Warnungen nach Datei
    const warningsByFile = {};
    for (const warning of results.warnings) {
      if (!warningsByFile[warning.file]) {
        warningsByFile[warning.file] = [];
      }
      warningsByFile[warning.file].push(warning);
    }
    
    for (const [file, warnings] of Object.entries(warningsByFile)) {
      summary += `### ${file}\n\n`;
      for (const warning of warnings) {
        summary += `- **Zeile ${warning.line}:** ${warning.pattern}\n`;
        summary += `  - Grund: ${warning.reason}\n`;
        summary += `  - Alternative: ${warning.alternative}\n`;
        
        console.log(`  ⚠️  ${warning.pattern} in ${file}:${warning.line}`);
      }
      summary += '\n';
    }
  }
  
  summary += '## 📝 Empfehlungen:\n\n';
  summary += '- ✅ Verwende Podman statt Docker Desktop (Apache 2.0, keine kommerziellen Einschränkungen)\n';
  summary += '- ✅ Prüfe alle Docker-Referenzen auf Podman-Alternativen\n';
  summary += '- ✅ Dokumentiere alle verwendeten Software-Lizenzen\n';
  summary += '- ✅ Verwende OCI-kompatible Tools (Podman, Buildah, Skopeo)\n\n';
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
  console.error('\n❌ Lizenzrechtlich problematische Software gefunden!');
  process.exit(1);
}

console.log('\n✅ Lizenzprüfung erfolgreich abgeschlossen');
process.exit(0);

