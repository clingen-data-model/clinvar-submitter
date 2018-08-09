# web-service in
* read body of POST as string, pass to critical path
* use defaults for options

# command-line in
* use options as specified
* slurp input interpretation, pass resulting string to critical path

# Critical path

## Input 
* string with data model variant interpretation
* options (with defaults)
  * method
  * citation
  * build

## Output
* map with two keys if success
  * result: String with CSV output of ClinVar submission
  * report: String with CSV output of submission report
* map with one key if validation fails
  * error: String with output of validation function
  
## Method

* Download schema
* Download context
* Validate input interpretation with schema
  * If fail, return error object, exit
  * On success, continue
* Construct CSV result string from data model input
* Construct CSV report string from data model input
* Return final output object


# web-service result

* generate JSON from return object
* return JSON in body of result

# command line result

* generate result file from result key
* generate result file from report key
