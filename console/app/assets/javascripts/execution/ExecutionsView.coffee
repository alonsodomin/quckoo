define ['knockout', 'jquery'], (ko, $) ->
  class ExecutionsView
    constructor: () ->
      @baseUri = '/executions'
      @socketUrl = jsRoutes.controllers.ExecutionController.executionsWs().webSocketURL()
      @executions = ko.observableArray()
      console.log "Connecting to WebSocket: " + @socketUrl
      @socket = new WebSocket(@socketUrl)
      @initialize()

    initialize: () ->
      $.get @baseUri + '/history', (result) =>
        $.each result, (idx, item) =>
          @executions.push item
      @socket.onmessage = (msg) ->
        console.log "Received: " + JSON.stringify(msg)