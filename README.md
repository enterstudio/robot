# Robot
Simple, easy to use, robust models for you android app.

### Principles

1. APIs should look synchronous and be backed by an event bus. Reason:
   to avoid verbosity and sometimes you don't really care for the
   callback
2. notifyChanges method should be used on rare ocations, the model
   should be able to determine by itself when a change has occurred and
   notify the change.

