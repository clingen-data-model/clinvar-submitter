<!--
<style>
  .markdown-body table {
    font-size: 12px !important;
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
clinvar-submitter-run-report.csv
  
  Date/Time:            9/11/2017 13:20  
  File(s):              dmwg1.json  dmwg2.json dmwg3.json  
  Method Name (-m):		  ACMG Guidelines, 2015 
  Method Citation (-c): PMID:25741868
  JSON-LD Context (-x):	http://datamodel.clinicalgenome.org/interpretation/json/context  
  Output File (-o): 	  clinvar-submitter-variant.csv  
  Run Report File (-r): clinvar-submitter-run-report.csv
  Force overwrite (-f): False  


#### INPUT/OUTPUT

Record# File Name     Interp Id   Variant (alt desig)           Cell    Status    Code    Description                       
------- ------------- ----------  ----------------------------  ------  --------  ------  -------------	                   
      1 dmwg1.json		123-101   	                              7/25    Warning  *W-251	  Preferred variant not provided. 
      2 dmwg1.json		435-199     NM_000257.3(MYH7):c.1207C>T   8/36    Error    *E-403	  Interpretation evaluation date not provided.	
      3 dmwg1.json		383-827     NM_000257.3(MYH7):c.1207C>T   8/37	  Error    *E-501	  Invalid met criteria rules and/or strength codes.
      4 dmwg2.json		482-229     NM_000257.3(MYH7):c.2681A>G           Success
      5 dmwg3.json		272-927     NM_000257.3(MYH7):c.788T>C    10/31   Error    *E-301	  Condition disease code or name not provided.


**NOTES:** 

- All variant coordinates are based on build GRCh38 coordinates from the ClinGen Allele Registry.
- Please provide feedback from your experience so that we can improve the future drafting of clinvar submissions
- you will need to review columns AW, AX, & AK (collection method, allele origin & affected status) are required but not available from interpretation message. Please fill in before submitting.
