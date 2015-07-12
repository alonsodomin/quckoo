define ['knockout', 'jquery'], (ko, $) ->
  class ExecutionsView
    constructor: () ->
      @baseUri = '/executions'
      @executions = ko.observableArray()
      @socket = new WebSocket(jsRoutes.controllers.ExecutionController.executionsWs().webSocketURL())
      @initialize()

    initialize: () ->
      $.get @baseUri + '/history', (result) =>
        $.each result, (idx, item) =>
          @executions.push item
      @socket.onmessage = (msg) ->
        console.log "Received: " + JSON.stringify(msg)