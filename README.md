# clinvar-submitter
Clojure (java) application for parsing a DMWG Interpretation JSON-LD file and transforming it into a ClinVar Variant submission record. 

See release notes at the bottom of this page.

This project consists of

1) A linked-data utility (ld.clj) for providing XPath-like queries agains json-ld input files
2) A set of ClinVar submssion specific form parsing functions (form.clj)
3) A set of input/output procedures for orchestrating the transformation (core.clj)
4) The project.clj file for defining the Clojure project and build options.
5) Example input interpretation data files and a copy of the interpretation jsonld context file they use.
6) A copy of the ClinVar Submission Template spreadsheet on which this version of the application is aligned.
7) Two log files. First one contains information for user and the second log is for developers for debuging purpose.
8) Unit tests

## Setup Environment
This project is managed using [Leiningen](https://leiningen.org/), a tool and style that focuses on project automation and declarative configuration.  See the [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md) for more information.

This project can be downloaded and run using the `lein run` command-line tool or it can used to build a stand-alone executable file for distribution. In order to perform either of these steps, you must first install the tool following the instructions in the Install section found at [Leiningen](https://leiningen.org/). 

## Executing Conversions
### Command line using 'Lein Run'
To run a conversion using the command-line with the lein run command ...
```
$ lein run "-o" "<csv output file>" "-f" "<jsonld context file>" "<json input file>" 
```
for example... 
```
$ lein run "-o" "myoutput.csv" "-x" "data/cg-interpretation.jsonld" "data/dmwg1.json" 
```
This will output the comma-separated clinvar-submitter records to a file called "myoutput.csv"
using the input file found at "data/dmwg1.json" and using the json-ld context file located at
"data/cg-interpretation.jsonld".

NOTE: See below for a full list and description of command line parameter options.

By default, the output file will not be overwitten. However, if this is desired, use the
"-f" like so...
```
$ lein run "-o" -"f" "myoutput.csv" "-x" "data/cg-interpretation.jsonld" "data/dmwg1.json" 
```


### Command line using executable jar
NOTE: This approach requires that the executable jar file exists. See Generating Executable Jar below for more information.

To run a conversion using the command-line with the executable jar ...
```
$ java -jar target/uberjar/clinvar-submitter-0.0.0-SNAPSHOT.jar "data/dmwg1.json" "-x" "data/cg-interpretation.jsonld" "-o" "myoutput.csv"
```
#### Command Line Parameters
```
"-o" "--output FILENAME" "CSV output filename"
"-x" "--jsonld-context URI" "JSON-LD context file URI"
"-f" "force-overwrite of output file if it exists"
"-b" "--build BUILD" "Genome build alignment, GRCh37 or GRCh38"
"-r" "--report FILENAME" "Run-report filename"
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
Created .../clinvar-submitter/target/uberjar/clinvar-submitter-0.1.0-SNAPSHOT.jar
Created .../clinvar-submitter/target/uberjar/clinvar-submitter-0.1.0-SNAPSHOT-standalone.jar
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
2.	Specifying Build (verify with Chris B. whether build info will be added to message)
3.	Does not fill in condition-name unless explicitly specified in condition.name field, not from disease-coding.display
4.	Does not handle multiple codings in condition.disease.
5.	No validation of terms used for criteria rules or strength. 
6.	Assumes a two-term space separated value for strength.
7.	No sorting of summarized met strength codes.
8.  No support for multiple file or directory input.
