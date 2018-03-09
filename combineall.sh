#!/bin/bash

# seed file with wrapper brace and VariantInterpretation array
rm -f tmpfile
echo '{"@context": "file:///Users/babbl/Development/clingen/clinvar-submitter/PAH_data/cg-interpretation.jsonld",' > allinput.json
echo '"VariantInterpretation":[' >> allinput.json

# Loop through product of gcsplit operation above...
for filename in dmwg*; 
do	
    printf "%s\n" "$filename"
		
    sed -e '2d' "$filename" > tmpfile		
    cat tmpfile >> allinput.json
    echo ',' >> allinput.json
		
done		

# remove last comma
sed -i '' -e '$ d' allinput.json 

# append enclosing bracket and brace to contain all VariantIntepretations.
echo ']}' >> allinput.json
rm -f tmpfile
