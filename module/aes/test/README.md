# AES Module - Testing Scripts

Script di test per validare le funzionalità del modulo AES tramite chiamate curl.

## Prerequisiti

- Applicazione in esecuzione (default: `http://localhost:8080`)
- Modulo `user` installato (per autenticazione JWT)
- Account utente valido (default: `test`/`test`, creato automaticamente durante l'import del modulo)
- Comandi: `curl`

## Variabili d'Ambiente

```bash
export API_BASE="http://localhost:8080"    # URL base API
export TEST_USERNAME="test"                # Username per login
export TEST_PASSWORD="test"                # Password per login
```

## Script Disponibili

### 1. aes_pdf_signature_field.sh
Test inserimento campo firma AcroForm in PDF esistente.
- **Endpoint**: `POST /api/aes/pdf/signature/field`
- **Input**: PDF base64, placeholder, dimensioni campo firma, partner (savino|namirial)
- **Output**: Hash SHA-256 del documento e numero di placeholder sostituiti
- **Storage**: `documents/YYYY/MM/DD/partner/hash.pdf`

```bash
./aes_pdf_signature_field.sh
```

Il file viene salvato in `storage/aes/documents/YYYY/MM/DD/savino/hash.pdf` e l'hash viene salvato in `/tmp/aes-cli-test/last-pdf-hash.txt` per uso successivo.

### 2. aes_html_signature_field.sh
Test conversione HTML → PDF con campo firma.
- **Endpoint**: `POST /api/aes/html/signature/field`
- **Input**: HTML string, placeholder, dimensioni campo firma, partner (savino|namirial)
- **Output**: Hash SHA-256 del documento e numero di placeholder sostituiti
- **Storage**: `documents/YYYY/MM/DD/partner/hash.pdf`

```bash
./aes_html_signature_field.sh
```

Il file viene salvato in `storage/aes/documents/YYYY/MM/DD/savino/hash.pdf` e l'hash viene salvato in `/tmp/aes-cli-test/last-html-hash.txt` per uso successivo.

### 3. aes_file_upload.sh
Test upload file in storage temporaneo.
- **Endpoint**: `POST /api/aes/file/upload`
- **Input**: File multipart, subfolder
- **Output**: Path file salvato (YYYY/MM/subfolder/hash-timestamp.ext)

```bash
./aes_file_upload.sh
```

Salva il path del file caricato in `/tmp/aes-cli-test/last-uploaded-file.txt` per uso successivo.

### 4. aes_file_download.sh
Test download file da storage temporaneo.
- **Endpoint**: `GET /api/aes/file/download?path=...`
- **Input**: Path file (opzionale, usa ultimo upload se omesso)
- **Output**: Contenuto file

```bash
# Usa ultimo file caricato
./aes_file_download.sh

# Usa path specifico
./aes_file_download.sh "2026/03/test-cli/abc123-1234567890.txt"
```

## Flusso di Test Completo

```bash
# 1. Test firma PDF con storage definitivo
./aes_pdf_signature_field.sh

# 2. Test conversione HTML → PDF con firma e storage definitivo
./aes_html_signature_field.sh

# 3. Upload file temporaneo
./aes_file_upload.sh

# 4. Download file appena caricato
./aes_file_download.sh
```

## Architettura Storage

Il modulo AES utilizza una struttura di storage separata per documenti definitivi e file temporanei:

### Documenti Firmati (Definitivi)
- **Path**: `storage/aes/documents/YYYY/MM/DD/partner/hash.pdf`
- **Partner**: `savino` o `namirial` (piattaforme firma remota)
- **Hash**: SHA-256 del contenuto PDF originale (usato come nome file)
- **Lifecycle**: Pronti per invio al partner → dopo firma spostati in `documents/YYYY/MM/DD/partner/signed/hash.pdf`

### File Temporanei
- **Path**: `storage/aes/tmp/YYYY/MM/subfolder/hash-timestamp.ext`
- **Retention**: Cleanup automatico dopo `aes.temp.retention.days` giorni (default: 7)
- **Uso**: Upload temporanei, elaborazioni intermedie

## Autenticazione

Tutti gli script:
1. Eseguono login a `/api/user/auth/login` con le credenziali configurate
2. Salvano i cookies JWT in `/tmp/aes-cli-test/cookies.txt`
3. Usano i cookies per tutte le chiamate successive (`-b cookies.txt`)

Il token **non** viene salvato tra esecuzioni - ogni script esegue un nuovo login.

## Verifica Test

Gli script verificano automaticamente:
1. **Login riuscito**: Presenza del cookie `access_token`
2. **Risposta API**: Campo `"err":false` nella response JSON
3. **File salvato**: Esistenza del file nel path previsto
4. **Dimensione file**: Verifica che il file non sia vuoto

## File Temporanei

Gli script usano `/tmp/aes-cli-test/` per file temporanei:
- `cookies.txt` - Cookies JWT per autenticazione
- `last-pdf-hash.txt` - Hash ultimo PDF firmato
- `last-html-hash.txt` - Hash ultimo PDF generato da HTML
- `last-uploaded-file.txt` - Path ultimo file caricato in tmp
- `request.json` - Request JSON per endpoint HTML

## Note

- Gli endpoint richiedono tutti autenticazione JWT
- I documenti firmati sono identificati da hash SHA-256 (univoco e deterministico)
- Il partner (savino/namirial) determina la sottocartella di destinazione
- Lo storage è persistente (bind-mounted da `./storage/aes/`)
