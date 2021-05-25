BEGIN
  DECLARE repo_name STRING;
  SET repo_name = (
    SELECT regexp_replace('clinvar_'||release_date||'_v'||lower(pipeline_version),r'[\.\-]','_') as name
    FROM broad-datarepo-terra-prod-cgen.datarepo_broad_dsp_clinvar.processing_history as ph
    ORDER BY release_date DESC
    LIMIT 1
    );
  EXECUTE IMMEDIATE format ("""
    CREATE OR REPLACE TABLE clingen-dx.clinvar_qa.vci_localkey_scv_map AS 
    SELECT scv.id as scv_id,
      REGEXP_EXTRACT(scv.local_key, r'^[a-z0-9-]{36}') as vci_interp_id,
      scv.local_key, 
      scv.variation_id, 
      scv.variation_archive_id, 
      scv.interpretation_date_last_evaluated, 
      subm.submission_date, 
      scv.submitter_id, 
      s.current_name
    from broad-datarepo-terra-prod-cgen.%s.clinical_assertion scv 
    join broad-datarepo-terra-prod-cgen.%s.clinical_assertion_variation scv_var on scv.id = scv_var.clinical_assertion_id
    join broad-datarepo-terra-prod-cgen.%s.submitter s on s.id = scv.submitter_id 
    join broad-datarepo-terra-prod-cgen.%s.submission subm on subm.id = scv.submission_id 
    WHERE lower(s.current_name) like '%%expert%%' or lower(s.current_name) like '%%broad%%'
  """, repo_name, repo_name, repo_name, repo_name);
END;
