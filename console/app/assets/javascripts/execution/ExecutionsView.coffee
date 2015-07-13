define ['knockout', 'jquery'], (ko, $) ->
  class ExecutionsView
    constructor: () ->
      @baseUri = '/executions'
      @socketUrl = jsRoutes.controllers.ExecutionController.executionsWs().webSocketURL()
      @executions = ko.observableArray()
      console.log "Connecting to WebSocket: " + @socketUrl
      @socket = new WebSocket(@socketUrl)
      @socket.onopen = () =>
        console.log "WebSocket opened with: " + @socketUrl
      @socket.onmessage = (msg) =>
        console.log "Received: " + JSON.stringify(msg)
      @initialize()

    initialize: () ->
      $.get @baseUri + '/history', (result) =>
        $.each result, (idx, item) =>
          @executions.push item
