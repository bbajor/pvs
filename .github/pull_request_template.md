## Ziel
Kurzbeschreibung der Änderung

## Checks
- [ ] Branch basiert auf `dev`
- [ ] PR-Ziel ist `dev` (Ausnahme: Maintainer-Merges zu `test`/`master`)
- [ ] Branch-Name folgt `cursor/<agent>/<topic>` (falls Agent)
- [ ] Kein autogenerierter Content / keine Build-Artefakte im Diff
- [ ] Lokal gebaut: `./gradlew build --no-daemon` ✅
- [ ] Lokal getestet: `./gradlew test --no-daemon` ✅
- [ ] README/Docs aktualisiert (falls nötig)
- [ ] Security-Check (keine Secrets/Keys/Passwörter im Code/Configs)

## Notizen
Screens / Hinweise

