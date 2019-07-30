# Shared Snap!

## Team members
Marco Canducci - marco.canducci@studio.unibo.it       
Daniele Schiavi - daniele.schiavi@studio.unibo.it  

## Course reference
https://www.unibo.it/it/didattica/insegnamenti/insegnamento/2018/430273

## A Computational Thinking Project

The *Shared Snap!* project wants to combine [Berkeley's Snap!](https://snap.berkeley.edu/) learning tool with a custom RESTful Web Server in order to allow microworlds intercommunication. 

Given the absence of this feature in the *Snap!* environment, we decided to tackle this challenge: we think that introducing communication between microworlds residing in different browser windows (or even in different machines!) could be very fun and engaging, allowing for tons of new possibilities. Already existing *Snap!* official libraries allow to make RESTful requests and to interact with JSON objects, therefore the choice of a RESTful Web Server came up naturally.

Keeping in mind the typical *Snap!* userbase, the main focus throughout the whole development process was to make the user experience as smooth and simple as possible. We decided to allow the exchange of two types of message content: one as plain text and the other as a stringified JSON object.

The format of the message to be sent to the server:

```
{
    "sender": "Daniele",
    "recipient": "Marco",
    "message": { ... }
}
```

The image below shows the *sequence diagram* for the data exchanged between the client actor (the one who wants to communicate with another actor remotely), the Walkie Talkie actor developed by us, the custom Web Server.

![alt text](https://raw.githubusercontent.com/candoz/shared-snap/master/images/sequence-diagram.png "Sequence diagram")

## Server side

First of all, we thought of modeling the *connection* as something to do in order to allow the sending and reception of messages to and from other users connected to the same server.

We decided to introduce at least a simple authentication system that assign an unique *Token* to every user at the moment of the connection to the server. That token must be included by the client in every subsequent request inside the *Authorization* header. To send a message, it's mandatory to be connected to the server, providing your authentication token along with the recipient's nickname.

The custom Web Server has been developed with Kotlin language, implementing a set of previously defined RESTful API which are available at the following address: <br />

https://app.swaggerhub.com/apis-docs/candoz/shared-snap/1.0#/


### Server execution requirements

- Java 8 must be installed
- a MongoDB daemon must be running on `localhost:10000`

### Deployment

It is possible to download the source code and the executable jar at the following page:

https://github.com/candoz/shared-snap/releases

In order to run this service you must execute the following command:
```
java -jar shared-snap-1.0-all.jar
```

## Client side

We started the client development by experimenting with two official *Snap!* libraries publicly available: the one that allows RESTful requests and the one that permits smooth interaction with JSON files. First of all we decided to extend the library for RESTful requests in order to make it possibile to read both the *body* and the *http response code* obtaining a more powerful API. This way, our Walkie Talkie actor can be able to always know and report back the reason of an eventual request failure. 

From there, we added a new abstraction layer through a set of new *Snap!* blocks, useful for the interaction with our custom Web server.

[walkie talkie actor](client/walkie%20talkie.xml)

![alt text](https://raw.githubusercontent.com/candoz/shared-snap/master/images/local-walkie-talkie-blocks.png "Local walkie talkie blocks")

Finally, we added an additional abstraction layer with the purpose of hiding technicalities such as the polling and authentication system behind the *Walkie Talkie actor*, allowing the use of the application even by less experienced users.

![alt text](https://raw.githubusercontent.com/candoz/shared-snap/master/images/public-api.png "Public API")

The *Walkie Talkie actor* encapsulates the previous calls to the service and offers simple a API for connecting, disconnecting, sending and receiving messages.

When the ball passes from one world to another the message exchanged between the two contains: the position y, the module of the velocity vector, the angle of the velocity vector and the color of the ball.

```
{
    "y": ...,
    "speedAngle": ...,
    "speedModule": ...,
    "color": ...
}
```

### Client execution requirements

- The server must be running.
- It's strongly recommended to use the Chrome browser (or Chromium derivates) to avoid problems with CORS requests.

### Prototype demonstration

To showcase our project functionalities we decided to implement a public demo that shows how a ball can bounce from one microworld to another.

![alt text](https://raw.githubusercontent.com/candoz/shared-snap/master/images/demo.png "Demo")

To run the demo, remember to start the Web Service first, and then open the following links.

Left screen:  
https://snap.berkeley.edu/snap/snap.html#present:Username=ct-unibo-ce-1819&ProjectName=Canducci%20Schiavi%20-%20left%20-%20Shared%20Snap%20Actor  

Right screen:  
https://snap.berkeley.edu/snap/snap.html#present:Username=ct-unibo-ce-1819&ProjectName=Canducci%20Schiavi%20-%20right%20-%20Shared%20Snap%20Actor       
