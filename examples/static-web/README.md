# Setup the example

This is the most straigthforward example to setup in order to test the Matomo CF service. What you need is adapting the deployment to your context and then launch that deployment through the relevant script:

```
./deploy.sh
```

For adapting to your context, you can either customize the four variables defined at the head of `deploy.sh` script or create a `context.sh` file that defines these four variables:

* APPNAME: this is the name of the CF app that will be deployed for this example.

* MATOMOSERVINST: this is the name of the instance of the Matomo service that will be ccreated to track this example application.

* BUILDPACK: this the name of the [staticfile](https://docs.cloudfoundry.org/buildpacks/staticfile/index.html) buildpack as instanciated within your own CloudFoundry platform.

* DOMAIN: this the domain within which the application will be exposed by your platform. This means that the complete route to this example application will be `$APPNAME.$DOMAIN`.

Example of a `context.sh` file:

```
#!/bin/sh

APPNAME=static-web
MATOMOSERVINST=sw_matomo
BUILDPACK=tf-static-buildpack
DOMAIN=cf.mycompany.org
```

Looking at deploy.sh script into more details, a `-c|--clean` is available if a complete reinitialization of this test environment is needed.

# Check Matomo is active

As soon as the example application is up and running, you can navigate it to test that it is tracked by Matomo.

After a few clicks, just go to your Matomo instance and connect with the credentials you can find in VCAP_SERVICES of the deployed example application. If all is running well, you should find that at least one visit has been tracked by Matomo.

# Lessons learnt

Apart from a basic Matomo setting for a static Web application, an interesting point that can be capitalized with this example is the ability to automatically inject credentials into the Matomo tracking code (through the rewritten template file `static/scripts/matomoInst.tpl.js`). Indeed a `matomoInst.js` file is generated and is embedded within the application (see files `ìndex.html` and `otherpage.html`. You can then have a look at `.profile` file which is executed each time the application is started by Cloudfoundry that implements such injection. The same kind of mechanism can be used for client-side GUI (for example a Single Page Application style) such as HTML5/JS implementations (e.g., an Angular implementation).
