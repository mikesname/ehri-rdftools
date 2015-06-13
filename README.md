EHRI JSON to RDF converter
==========================

Converts vertex/edge/property data to RDF triples.

Data is not currently aligned to any particular ontology; this is solely for testing purposes.

Accepts input data of the (somewhat ad-hoc) form:

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
                ["another-id-1", "another-type-1"]                
            ]
        }
    }
]
```

To build:

    sbt assembly
   
To run:

    java -jar target/scala-2.11/json2rdf-assembly-0.1-SNAPSHOT.jar -i input-data.json
     
Alternately you can feed stdin.
     
Different RDF output formats can be set with the -f option. The default is `ttl` (Turtle). Other options are:
      
 - xml (RDF+XML)
 - n3
 - rj (RDF/JSON) - **Warning**: doesn't stream and thus uses an amount of memory proportional to the input file.        
