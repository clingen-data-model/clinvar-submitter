<!--
<sub>...</sub> is used to make font size small
<style>
  .markdown-body table td {
    font-size: 8px !important;
}
</style>
-->

## Instruction to request/receive clinvar submissions for clingen users:

**Request Process:** 
Please email your request including input interpretation data files to someone@partners.org. 

**Response:**
After we receive your request someone will process it and will send you the following:
- Output .csv file
- Run Report

Please review the run report for any issue with your provided input. If there are issues in the input file please fix and re-send your request.

**ClinVar-Submitter sample run report** 
clinvar-submitter-run-report.txt
  
**Date/Time:** 				9/11/2017 13:20  
**Directory:** 				~/Documents/clinvar-submitter/2017-09-11   
**File(s):**                           * badfile.json  * dmwg1.json  * dmwg2.json  * dmwg3.json  
**Method Name (-m):**		        ACMG Guidelines, 2015 ..  
**Method Citation (-c):** 	        PMID:25741868 ..  
**JSON-LD Context (-x):**	        http://datamodel.clinicalgenome.org/interpretation/json/context  
**Output File (-o):** 		        clinvar-submitter-variant.csv  
**Run Report File (-r):** 	        clinvar-submitter-run-report.txt  
**Force overwrite (-f):** 	        no  


#### INPUT/OUTPUT

|Result#    |File Name      |Record|Variant (alt desig)         |Cell    |Status |Code    |Description                                      |
|-------	|-------		|---   |-------------------		    |-----	 |----	 |------  |-------------	                                |
|**101**	|badfile.json	|-	   |-							|	A6	 |Error	 |E-201	  |Unable to process file badfile.json.             |
|**102**	|dmwg1.json		|1	   |-							|	Y7	 |Warning|W-251	  |Preferred variant not provided.                  |
|**103**	|dmwg1.json		|2	   |NM_000257.3(MYH7):c.1207C>T |	AK8	 |Error	 |E-403	  |Interpretation evaluation date not provided      |	
|**104**	|dmwg1.json		|2	   |NM_000257.3(MYH7):c.1207C>T |	AQ8	 |Error	 |E-501	  |Invalid met criteria rules and/or strength codes |
|**105**	|dmwg2.json		|1	   |NM_000257.3(MYH7):c.2681A>G |	9	 |Success|		  |	                                                |
|**106**	|dmwg3.json		|1	   |NM_000257.3(MYH7):c.788T>C	|	AE10 |Error	 |E-301	  |Condition disease code or name not provided.	    |



**NOTES:** 

- All variant coordinates are based on build GRCh38 coordinates from the ClinGen Allele Registry.
- Please provide feedback from your experience so that we can improve the future drafting of clinvar submissions
- you will need to review columns AW, AX, & AK (collection method, allele origin & affected status) are required but not available from interpretation message. Please fill in before submitting.
