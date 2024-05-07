.. _twc:

TWC
---

The TWC Module provides integration with NoMagic's Teamwork Cloud. For MDK SSO login integration, the MDK and TWC must be configured.

TWC: TWC SSO must be configured correctly and working via the Single-Sign On tab when logging in to TWC from Cameo

MDK: Under Options -> Environment -> MDK -> Set MMS Authentication Chain, add `org.openmbee.mdk.tickets.TWCAcquireTicketProcessor` to the front

MMS: If the TWC api has a self-signed or private SSL cert, ensure the Java runtime running MMS has the root cert in its keystore

Configuration
^^^^^^^^^^^^^

The TWC Module supports multiple instances of TWC. The following configuration are available, where twc.instances is a list.

  twc.instances[i].url
    The url of the REST interface of TWC.

  twc.instances[i].protocol
    The protocol to use for the REST interface of TWC.

  twc.instances[i].port
    The port for the REST interface of TWC.

  twc.instances[i].aliases
    A list of aliases for the REST interface of TWC.

  twc.instances[i].adminUsername
    The admin username to use with the defined instance. Required.

  twc.instances[i].adminPwd
    The admin password to use with the defined instance. Required.

  twc.instances[i].roles.project_read
    The TWC roles to use for MMS project_read permissions.

  twc.instances[i].roles.project_read_commits
    The TWC roles to use for MMS project_read_commits permissions.

  twc.instances[i].roles.project_read_permissions
    The TWC roles to use for MMS project_read_permissions permissions.

  twc.instances[i].roles.project_edit
    The TWC roles to use for MMS project_edit permissions.

  twc.instances[i].roles.project_create_branch
    The TWC roles to use for MMS project_create_branch permissions.

  twc.instances[i].roles.project_update_permissions
    The TWC roles to use for MMS project_update_permissions permissions.

  twc.instances[i].roles.branch_read
    The TWC roles to use for MMS branch_read permissions.

  twc.instances[i].roles.branch_read_permissions
    The TWC roles to use for MMS branch_read_permissions permissions.

  twc.instances[i].roles.branch_edit_content
    The TWC roles to use for MMS branch_edit_content permissions.

  twc.instances[i].roles.branch_update_permissions
    The TWC roles to use for MMS branch_update_permissions permissions.