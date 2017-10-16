# subs-message-recover

[![Build Status](https://travis-ci.org/EMBL-EBI-SUBS/subs-message-recover.svg?branch=master)](https://travis-ci.org/EMBL-EBI-SUBS/subs-message-recover)

This applications provides functionality/template to replay a previously failed message to the original exchange with their original routing_key.
It uses a third party application (QDB) for storing the messages.

### The list of the message flow is the following:

1. The rejected message arrives to the Dead Letter Queue (DLQ).
2. The message replay application transfer the arrived message to its own queue under the QDB database
3. The user can filter the messages using regular expression on the message body, by routing key or by a from and/or to date pattern.
4. To deal with the filtered messages there are more options:
   - the user would like to replay the same messages - do nothing with the message(s)
     - a dependent service stopped working, but now it is recovered
     - there was a bug in the consumer of the message and it has been fixed
   - the message is missing 1 or more keys/values - the user should replace the `fixFailedMessages` method to add some code to do the modification with the message(s)
5. Now we have the messages to replay that we will send to the original exchange with their original routing key


### How to configure the application

The user can configure the application through the application.yml configuration file that could be found under the src/main/resources folder.

The settings can be found under the `messageRecover` key.

The list of the settings:

- inputBindingRemovalDelayInSec: set the delay between set up input binding and remove it (in seconds) 
- rabbitMQProp section:
  - exchangeName: the name of the RabbitMQ exchange the user would like to connect to
  - deadLetterExchangeName: the name of the RabbitMQ Dead Letter Exchange the user would like to connect to
  - deadLetterQueueName: the name of the RabbitMQ Dead Letter Queue the user would like to connect to
- qdbProp section:
  - baseURL: QDB server URL
  - queue section:
    - deadLetterQueueName: the name of the permanent QDB queue for storing the messages
    - basePath: DO NOT MODIFY IT - This is a QDB internal URI path fragment, default to: `/q`
    - inputBasePath: DO NOT MODIFY IT - This is a QDB internal URI path fragment, default to: `/in`
    - outputBasePath: DO NOT MODIFY IT - This is a QDB internal URI path fragment, default to: `/out`
    - inputPath: this is an binding name to transferring the messages from a RabbitMQ to a QDB queue, default to: `/fromRabbit`
    - outputPath: this is an binding name to transferring the messages from a QDB to a RabbitMQ queue, default to: `/toRabbit`
    - maxSize: The maximum size of the QDB queue. If the queue exceeds this limit, then the older messages will be deleted.
    - maxPayloadSize: The maximum size of the message payload.
    - contentType: the content type of the payload, default to : `application/json; charset=utf-8`
  - messageFilter section: This is the section where you can set up filters for messages to replay
    - grep: to add a regulare expression to filter body of the message
    - from: filter those messages whose published date is equals or later than this setting  
    - to: filter those messages whose published date is equals or earlier than this setting
    - routingKey: filter messages by routing key
    - fromId: filter messages from QDB internal ID
