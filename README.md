# civ6hook

A simple Clojure server for listening to incoming Civilization VI Play by Cloud webhook requests
and sending email turn notifications.

## Running in development mode

To start a web server for the application, copy settings-example.edn to settings.edn and adjust as needed.
Then run:

    lein ring server

In develop mode, you can access a dev helper site at

    http://localhost:3000/

## Production build

    lein ring uberjar
