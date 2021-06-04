# Find SCV by Submitter Local Key
from google.cloud import bigquery
import json

def findByLocalKey(request):

    request_json = request.get_json()
    if request.args and 'local_key' in request.args:
        local_key = request.args.get('local_key')
    elif request_json and 'local_key' in request_json:
        local_key = request_json['local_key']
    else:
        return ("required argument 'local_key' missing",400)
    
    client = bigquery.Client()

    # Perform a query.
    QUERY = (f"SELECT scv_id  FROM `clingen-dx.clinvar_qa.vci_localkey_scv_map` WHERE vci_interp_id = '{local_key}'")

    print(QUERY)

    query_job = client.query(QUERY)  # API request
    rows = query_job.result()  # Waits for query to finish

    if rows.total_rows != 1:
        return ("no exact match found for local_key",400)
  
    return serialize_bq_row_iterator(rows)[0]['scv_id']

def serialize_bq_row_iterator(row_iterator:bigquery.table.RowIterator): 
    df = row_iterator.to_dataframe()
    j = df.to_json(orient="records")
    return json.loads(j)


