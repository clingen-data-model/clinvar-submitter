# clinvar-submitter

Clojure (java) application for parsing the ClinGen Variant Pathogenicity Interpretation msessages in JSON-LD form and transforming it into a ClinVar Variant submission record. The primary benefits are to improve consistency and reduce time to draft a ClinVar Submission *Variant* sheet. It has features for error checking and warnings, as well as, consistent evidence summary description generation containing the ACMG ISV criterion rules. 

This project consists of

* project.clj - Clojure project and build options.
* [Source code](src) 
** ld.clj - A linked-data utility for providing XPath-like queries agains json-ld input files
** form.clj - A set of ClinVar submssion specific form parsing functions 
** core.clj - A set of input/output procedures for orchestrating the transformation
** report.clj - The functions for writing out the output and run report.
* [Unit Tests](test/clinvar-submitter) 
* [Data files](data)
** Example json files - input interpretation data files 
** Clinvar Submission Template - A copy of the ClinVar Submission Template spreadsheet on which this version of the application is aligned. Also available at the ClinVar website.
* [Developer Documentation](doc)
* combineall.sh - an optional script to combine multiple interpretation json files into a single json file. 

## Setup Environment
This project is managed using [Leiningen](https://leiningen.org/), a tool and style that focuses on project automation and declarative configuration.  See the [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md) for more information.

This project can be downloaded and run using the `lein run` command-line tool or it can used to build a stand-alone executable file for distribution. In order to perform either of these steps, you must first install the tool following the instructions in the Install section found at [Leiningen](https://leiningen.org/). 

## Executing Conversions
### Command line using 'Lein Run'
To run a conversion using the command-line with the lein run command ...
```
$ lein run "-o" "<csv output file>" "<json input file>" 
```
for example... 
```
$ lein run "-o" "myoutput.csv"  "data/dmwg1.json" 
```
This will output the comma-separated clinvar-submitter records to a file called "myoutput.csv"
using the input file found at "data/dmwg1.json".

By default, the output file will not be overwitten. However, if this is desired, use the
"-f" like so...
```
$ lein run "-o" -"f" "myoutput.csv"å "data/dmwg1.json" 
```


### Command line using executable jar
NOTE: This approach requires that the executable jar file exists. See Generating Executable Jar below for more information.

To run a conversion using the command-line with the executable jar ...
```
$ java -jar target/uberjar/clinvar-submitter-0.0.0-SNAPSHOT.jar "-o" "myoutput.csv" "data/dmwg1.json" 
```
#### Command Line Parameters
```
"-o" "--output FILENAME" "CSV output filename" : default "clinvar-submitter.csv"
"-x" "--jsonld-context URI" "JSON-LD context file URI" :default "http://datamodel.clinicalgenome.org/interpretatoin/json/sepio_context"
"-f" "force-overwrite of output file if it exists"
"-b" "--build BUILD" "Genome build alignment, GRCh37 or GRCh38" :default "GRCh37"
"-r" "--report FILENAME" "Run-report filename" :default "clinvar-submission-run-report.csv"
"-m" "--method METHODNAME" "Assertion-method-name" :default "ACMG Guidelines, 2015"
"-c" "--methodc METHODCITATION" "Method Citation" :default "PMID:25741868"
```

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
