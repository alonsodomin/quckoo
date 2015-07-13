define ['knockout', 'jquery'], (ko, $) ->
  class ExecutionsView
    constructor: () ->
      @baseUri = '/executions'
      @socketUrl = jsRoutes.controllers.ExecutionController.executionsWs().webSocketURL()
      @executions = ko.observableArray()
      @socket = new WebSocket(@socketUrl)
      @socket.onmessage = (msg) =>
        console.log "Received: " + JSON.stringify msg
        if (msg.data.event && msg.data.event == 'execution')
          @executionEvent(msg.data)
      @initialize()

    initialize: () ->
      $.get @baseUri + '/history', (result) =>
        $.each result, (idx, item) =>
          @executions.push item

    executionEvent: (event) ->
      @executions[event.executionId] = event