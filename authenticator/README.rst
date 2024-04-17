
Authenticator
-------------

This module leverages Spring Security's authentication manager to try all registered auth checks provided with other modules


* Adds login endpoint that returns JWT (JSON Web Token)

  * token is generated from username and authorities as returned by authentication manager

* Adds JWT Bearer token auth

References
^^^^^^^^^^


* `Overview of Spring Security <https://spring.io/guides/topicals/spring-security-architecture#_authentication_and_access_control>`_
* `UserDetails <https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/userdetails/UserDetails.html>`_
* `Authentication <https://docs.spring.io/spring-security/site/docs/current/api/org/springframework/security/core/Authentication.html>`_
* :ref:`localauth`
* :ref:`ldap`