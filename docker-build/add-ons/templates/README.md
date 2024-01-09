# Templates

- **tomcat_orig**
  - contains files as shipped with tomcat
  - Loginbuddy uses modified copies of those
- **configTemplates.json**
  - a template containing providers that are supported out of the box from a configuration point of view
  - the **redirect_uri** is set to **https://local.loginbuddy.net/callback**. This may be used during development
  - **config.json** generally only need to overwrite these keys:
    - **provider**, **client_id**, **client_secret**, **redirect_uri**
  - this file is also part of **saschazegerman/loginbuddy:latest**
- **permissionsTemplate.policy**
  - this file contains the permissions that may be added to support a templated provider