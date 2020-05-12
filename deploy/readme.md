# Deployment steps for ClinGen stage:

* Uses ansible to deploy (specifically ansible-playbook command) 
* From clinvar-submitter/deploy git project directory

```
> ansible-playbook -i stage.yml clinvar-submitter.yml
```

* Clinvar-submitter.yml references clinvar-submitter.service

After that you should be able to point your browser to: 35.196.45.74 and you should see the message:

> Not found. Perhaps you meant to use a URI of '/api/v1/submission'.

That will at least let you know that it deployed successfully.
