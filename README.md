[![Build Status](https://travis-ci.org/mikesname/ehri-rdftools.svg?branch=master)](https://travis-ci.org/mikesname/ehri-rdftools)

EHRI JSON to RDF converter
==========================

Converts vertex/edge/property data to RDF triples, in a streaming manner conducive to batch-processing
an entire database.

Data is not currently aligned to any particular ontology; this is solely for testing purposes.

Accepts input data of the (somewhat ad-hoc) form (exported by the [EHRI Web Service](https://github.com/EHRI/ehri-rest)
since version 0.10.2):

```
[
    {
        "id": "some-id",
        "type": "some-type",
        "data": {
            "key": "value"
        },
        "relationships": {
            "relName1": [
                ["another-id-1", "another-type-1"]
            ],
            "relName2": [
                ["another-id-2", "another-type-2"]
            ]
        }
    }
]
```

this outputs something like:

```
@prefix ehri: <http://ehri-project.eu/> .

<http://ehri-project.eu/some-type#some-id> a ehri:some-type ;
	ehri:key "value" ;
	ehri:relName1 <http://ehri-project.eu/another-type-1#another-id-1> ;
	ehri:relName2 <http://ehri-project.eu/another-type-2#another-id-2> .
```

i.e. assuming the prefix, class URIs are constructed from `PREFIXtype`,
 identity URIs from `PREFIXtype#id`, relationship URIs from `PREFIXlabel`,
 and property URIs from `PREFIXkey`.

(Obviously this is all subject to change...)

To build:

    sbt assembly
   
The assembly is executable. You can run the uberjar directly with:

    ./target/scala-2.11/json2rdf-assembly-0.1-SNAPSHOT.jar -i input-data.json
     
Alternately you can feed stdin instead of using the `-i [FILE]` option.
     
Different RDF output formats can be set with the -f option. The default is `ttl` (Turtle). Other options are:
      
 - xml (RDF+XML)
 - n3
 - rj (RDF/JSON) - **Warning**: doesn't stream and thus uses an amount of memory proportional to the input file.        
