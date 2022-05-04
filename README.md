# clinvar-submitter

Clojure (java) application for parsing the ClinGen Variant Pathogenicity Interpretation msessages in JSON-LD form and transforming it into a ClinVar Variant submission record. The primary benefits are to improve consistency and reduce time to draft a ClinVar Submission *Variant* sheet. It has features for error checking and warnings, as well as, consistent evidence summary description generation containing the ACMG ISV criterion rules. 

This project consists of

* project.clj - Clojure project and build options.
* [Source code](src) 
  * ld.clj - A linked-data utility for providing XPath-like queries agains json-ld input files
  * form.clj - A set of ClinVar submssion specific form parsing functions 
  * core.clj - A set of input/output procedures for orchestrating the transformation
  * report.clj - The functions for writing out the output and run report.
  * variant.clj - Functions that operate on the the submitted variants
  * web_service.clj -Functions to provide REST API web service.
* [Unit Tests](test/clinvar-submitter) 
* [Data files](data)
  * Example json files - input interpretation data files 
  * Clinvar Submission Template - A copy of the ClinVar Submission Template spreadsheet on which this version of the application is aligned. Also available at the ClinVar website.
  * Postman - This file contains importable files for the Postman (free program for testing REST APIs - https://www.getpostman.com/) program that will allow testing of the Web Service.
* [Developer Documentation](doc)
* combineall.sh - an optional script to combine multiple interpretation json files into a single json file. 
* gcp - Google Cloud Function and BigQuery artifacts

## Setup Environment
This project is managed using [Leiningen](https://leiningen.org/), a tool and style that focuses on project automation and declarative configuration.  See the [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md) for more information.

This project can be downloaded and run using the `lein run` command-line tool or it can used to build a stand-alone executable file for distribution. In order to perform either of these steps, you must first install the tool following the instructions in the Install section found at [Leiningen](https://leiningen.org/). 

This project runs as a webserver.

#### Environment Variables
CLINVAR_SUBMITTER_PORT - The port to run the service on. Defaults to 3000.
SCV_SERVICE_URL - The URL for the SCV cloud function. Must be provided. 


#### Error Handling
To handle general exceptions, standard clojure exception handling methods are used using try catch block.
But in some cases since we don’t want to jump out of our function with an exception, for example when an element of a json file does not exists,this app is returning either an error message, or a value. If there is an error, value is nil and error is a string error message. If no error occurred, the error message is nil.

#### Generating an Executable Jar.
To build an executable jar, run

```
$ lein uberjar
Compiling clinvar-submitter.core
Compiling clinvar-submitter.form
Compiling clinvar-submitter.ld
Created .../clinvar-submitter/target/uberjar/clinvar-submitter-1.0.0.jar
Created .../clinvar-submitter/target/uberjar/clinvar-submitter-1.0.0-standalone.jar
```

### Combining Multiple Input Files into a Single Conversion
The bash script file called combineall.sh is helpful if multiple json files containing single
interpretation records need to be combined into a single input file prior to running a 
conversion. Doing this will output all records at once to a single CSV output file if helpful.

WARNING: this bash script is a draft and as such ...
1. presumes all files to be combined live in the same directory and match the pattern dmwg*.json...
2. overwrite the hardcoded output filename to be allinput.json

To run...
```
$ ./combineall.sh
````
This will take all files in the same directory that match the pattern dmwg*.json and
write them to an output file called allinput.json.

## Import output to a ClinVar Submitter Form
After generating converted clinvar submitter output take the output file (in the examples above called myoutput.csv)
and either copy and past it or import it into the Variant Sheet of of the [ClinVarSubmissionTemplate.xlsx](ftp://ftp.ncbi.nlm.nih.gov/pub/clinvar/submission_templates/SubmissionTemplate.xlsx) file 
(a snapshot of which may be found in the data folder). 

## RELEASE NOTES 
Not supported in this release
1.	Phenotypes in Condition.
2.	Does not handle multiple codings in condition.disease.
3.	No validation of terms used for criteria rules or strength. 
4.	Assumes a two-term space separated value for strength.
5.	No sorting of summarized met strength codes.
6.  No support yet for multiple file or directory input.

### 04.May.2022
#### (fixed) #21 Modified the output to match March 2022 ClinVar submission excel file format column changes
*these are the specific column changes*
* (remove) Trace or probe data (V)
* (remove) Official allele name (AA)
* (remove) Condition category (AH)
* (added) Assertion score  (between Clin Sig & Date Last Eval)
* (remove) Number of chromosomes with variant (BQ)
* (remove) Citations or URLs that cannot be represented in evidence citations column (CA)
#### (fixed) #22 Clinvar Submitter Service is returning 400 error
Root cause: When calling the with variant that has no b38 or preferred name the clinvar-submitter service was throwing an uncaught exception
Resolution: Added capability to grab the b37 hgvs and refseq accession if b38 and preferred name is not available. As well, as embedded a new ERROR code *E-203 to handle exceptional situations when neither b38, b37 or preferred names exist. The new error code will be embedded in the output that is pasted in the corresponding `hgvs` and `refseq` columns if this occurs.
    
    
