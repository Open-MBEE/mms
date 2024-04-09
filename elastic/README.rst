.. _elastic:

Elastic
-------

Elasticsearch implementation for json document storage, indexing and search. Implements the ``*IndexDAO`` and ``SearchService`` interfaces from ``core``

On project creation, will look for a mapping file based on the project schema prefixed to '_node.json' under $classpath/elastic_mappings, otherwise will use elastic_mappins/default_node.json for field mapping

Configuration
^^^^^^^^^^^^^

The following are a list of options to configure the Elastic Module for MMS.

  elasticsearch.host
    The host name of the Elasticsearch server or cluster. Required.

  elasticsearch.port
    The port number of the Elasticsearch server or cluster. Required.

  elasticsearch.http
    The transport protocol to use to connect to the Elasticsearch server or cluster. Required.

  elasticsearch.username
    Username, Optional.

    | `Default: null`

  elasticsearch.password
    Password, Optional.

    | `Default: null`

  elasticsearch.limit.result
    The maximum number of results a single search request should return. Optional.

    | `Default: 10000`

  elasticsearch.limit.term
    The maximum number of terms that a search query should contain. Optional.

    | `Default: 1000`

  elasticsearch.limit.scrollTimeout
    The maximum time to wait for search requests. Optional.

    | `Default: 1000`

  elasticsearch.limit.get
    The maximum number of elements that a single get request should return. Optional.

    | `Default: 5000`

  elasticsearch.limit.index
    The maximum number of elements that will be indexed in a single bulk request. Optional.

    | `Default: 5000`

  elasticsearch.limit.commit
    The maximum number of elements to limit commit objects. Set this to a reasonable size in order to avoid object size limitations in Elasticsearch.

    | `Default: 10000`

Elastic Mappings
^^^^^^^^^^^^^^^^

Elastic mappings are necessary to index fields correctly in Elasticsearch. These mappings are available `here <https://github.com/Open-MBEE/mms/tree/develop/elastic/src/main/resources/elastic_mappings>`_.