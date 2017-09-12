# clinvar-submitter
Clojure (java) application for parsing a DMWG Interpretation JSON-LD file and transforming it into a ClinVar Variant submission record.

This project consists of

1) A linked-data utility (ld.clj) for providing XPath-like queries agains json-ld input files
2) A set of ClinVar submssion specific form parsing functions (form.clj)
3) A set of input/output procedures for orchestrating the transformation (core.clj)
4) The project.clj file for defining the Clojure project and build options.
5) Example input interpretation data files and a copy of the interpretation jsonld context file they use.
6) A copy of the ClinVar Submission Template spreadsheet on which this version of the application is aligned.
7) Two log files. First one contains information for user and the second log is for developers for debuging purpose.
8) Unit tests

This project is managed using [Leiningen](https://leiningen.org/), a tool and style that focuses on project automation and declarative configuration.  See the [tutorial](https://github.com/technomancy/leiningen/blob/stable/doc/TUTORIAL.md) for more information.

This project can be downloaded and run using the `lein run` command-line tool or it can used to build a stand-alone executable file for distribution. In order to perform either of these steps, you must first install the tool following the instructions in the Install section found at [Leiningen](https://leiningen.org/). 

I. Command-Line - To run a conversion using the command-line ...
```
$ lein run <option> <csv output file> <option> <jsonld context file> <json input file> 
Options:
"-o" "--output FILENAME" "CSV output filename"
"-c" "--jsonld-context URI" "JSON-LD context file URI"
```
for example... 
```
$ lein run "-o" "myoutput.csv" "-c" "data/cg-interpretation.jsonld" "data/dmwg1.json" 
```

II. Executable - To build an executable and run it to convert a file...
Step 1 - build executable
```
$ lein uberjar
Compiling clinvar-submitter.core
Compiling clinvar-submitter.form
Compiling clinvar-submitter.ld
Created .../clinvar-submitter/target/uberjar/clinvar-submitter-0.1.0-SNAPSHOT.jar
Created .../clinvar-submitter/target/uberjar/clinvar-submitter-0.1.0-SNAPSHOT-standalone.jar
```
Step 2 - run executable
```
$ java -jar target/uberjar/clinvar-submitter-0.0.0-SNAPSHOT.jar "data/dmwg1.json" "data/cg-interpretation.jsonld" "myoutput.csv"
```


after running either of the above example you should have a file called myoutput.csv which contains a csv formatted record which can be copied and pasted into a copy of the ClinVarSubmissionTemplate.xlsx file (also available in the data folder).  
