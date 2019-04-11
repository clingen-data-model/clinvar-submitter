# Clinvar-submitter REST Service

Service to convert VCI variant submission in SEPIO JSON format into a csv format for submission to ClinVar. The clinvar-submitter service is running in Google Cloud Platform (GCP).

## 1. REST Endpoint
  Current:
    http://35.196.45.74/api/v1/submission
	
  Future:
	  https://clinvar.clinicalgenome.org/api/v1/submission

Notice that the scheme currently is http: and the hostname is an IP address.  The hostname change in the near future once we have Amazon point our DNS record to our server in GCP, an we will begin using SSL/TLS once we have a reverse proxy server in place (or not) and certificates are installed.

## 2. Data submission 
The service accepts a JSON array of SEPIO VCI variant submissions:
```	
	JSON input array submitted via HTTP POST Method: 
	[
		;; JSON SEPIO array of VCI submission records
		{json record 1},
		{json record 2},
		…
 	]
```

## 3. Return data
The data returned by the web service is a JSON object containing a *status* object, and a *variants* array objects. The *status* object contains a count of records processed, a count of records with errors and a count of records processed that have errors. Each entry in the *variants* array contains a *submission* array of values corresponding to the ClinVar submission spreadsheet columns, and an *errors* array of values detailing the errors that have occurred in the variant submission.  If there are no errors, the *errors* array will be empty. If there are errors then there will be *errorCode* and *errorMessage* entries for each error in the variant submission. 

```
	JSON return structure: 
	{	
		status:	{
			totalRecords: 0,
			successCount: 0,
			errorCount: 0,
		}
		variants:  [
			{
				submission: []
				errors: []
			},
			{
				submission: []
				errors: [
					{
						errorCode: “…”
						errorMessage: “…”
					}
				]
			},
			…
		]	
	}
```

## 4. Postman Samples
Postman is a free application for testing REST APIs available at https://www.getpostman.com/. In the project *doc/Postman* directory are files that can be imported into Postman to test sample requests against the clinvar-submitter service.

## 5. Future Enhancements
- Support for SCV transmission (for updating ClinVar)
- Swagger/Open API UI interface
- URL parameters for
  - JSON LD Context
  - collection method
  - allele origin
  - affected status

