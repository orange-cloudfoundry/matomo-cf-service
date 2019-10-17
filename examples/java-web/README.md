# Setup the example

This is a dynamic Web application example (powered by Java SpringBoot and Thymeleaf) to setup in order to test the Matomo CF service in such a context. What you need is adapting the deployment to your context and then launch that deployment through the relevant script, as soon as the Java application has been compiled:

```
mvn install
./deploy.sh
```

For adapting to your context, you can either customize the four variables defined at the head of `deploy.sh` script or create a `context.sh` file that defines these four variables:

* APPNAME: this is the name of the CF app that will be deployed for this example.

* MATOMOSERVINST: this is the name of the instance of the Matomo service that will be ccreated to track this example application.

* BUILDPACK: this the name of the [Java](https://docs.cloudfoundry.org/buildpacks/java/) buildpack as instanciated within your own CloudFoundry platform.

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

This basic Matomo setting with a Java-based dynamic Web application shows a way to automatically inject credentials into the Matomo tracking code embedded into Web pages. Here it relies on the templating engine of Thymeleaf but similar mechanism should by used wathever the language runtime and the Web framework. As the runtime executes server-side, then credentials are available within the execution context (VCAP_SERVICES).
