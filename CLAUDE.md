# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Clojure application that transforms ClinGen Variant Pathogenicity Interpretation messages (in JSON-LD format) into ClinVar submission records. The primary purpose is to improve consistency and reduce time to draft ClinVar Submission Variant sheets.

## Architecture

The application is structured as both a command-line tool and a REST web service:

### Core Modules
- **core.clj** - Main entry point and orchestration 
- **web_service.clj** - REST API web service handlers using Ring/Jetty
- **ld.clj** - Linked-data utility providing XPath-like queries against JSON-LD files
- **form.clj** - ClinVar submission-specific form parsing functions
- **report.clj** - Output generation and run report functions  
- **variant.clj** - Operations on submitted variants
- **env.clj** - Environment configuration

### Google Cloud Integration
- **gcp/function-source/main.py** - BigQuery-based SCV lookup service
- **gcp/function-source/requirements.txt** - Python dependencies for cloud function

## Common Development Commands

### Building and Running
```bash
# Run the web service
lein run

# Build executable JAR
lein uberjar

# Run tests
lein test

# Start development server with auto-reload
lein ring server-auto
```

### Environment Variables
- `CLINVAR_SUBMITTER_PORT` - Service port (defaults to 3000)
- `SCV_SERVICE_URL` - Required URL for SCV cloud function

### Batch Processing
Use `generateSubmission.sh` for processing archived ClinGen interpretation files:
```bash
./generateSubmission.sh -d YYYY-MM-DD -g GROUP_NAME [-s SCV_FILE] [-o]
```

### Input File Combination
Use `combineall.sh` to combine multiple JSON interpretation files into a single input file (combines dmwg*.json files into allinput.json).

## Project Structure

- **src/clinvar_submitter/** - Main Clojure source code
- **test/clinvar_submitter/** - Unit tests  
- **data/** - Example JSON files, ClinVar template, and Postman collections
- **doc/** - Developer documentation and API specs
- **gcp/** - Google Cloud Platform artifacts (BigQuery, Cloud Functions)
- **deploy/** - Deployment configuration files

## Key Dependencies

- Clojure 1.9.0
- Ring (web server)
- Cheshire (JSON handling)
- jsonld-java (JSON-LD processing)
- clj-http (HTTP client)

## Output Formats

The application generates:
1. **CSV submission file** - Ready for ClinVar submission template
2. **Run report CSV** - Processing summary and validation results
3. **JSON output** - Intermediate processing results

## Testing

Tests are located in `test/clinvar_submitter/` and can be run with standard Leiningen commands. See `test/repl-test-tips.txt` for REPL testing guidance.