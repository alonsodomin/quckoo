define ['knockout', 'jquery'], (ko, $) ->
  class ExecutionsView
    constructor: () ->
      @baseUri = '/executions'
      @socketUrl = jsRoutes.controllers.ExecutionController.executionsWs().webSocketURL()
      @executions = ko.observableArray()
      @socket = new WebSocket(@socketUrl)
      @socket.onmessage = (msg) =>
        @processWsMessage JSON.parse(msg.data)
      @initialize()

    initialize: () ->
      $.get @baseUri + '/history', (result) =>
        $.each result, (idx, item) =>
          @executions.push item

    executionEvent: (event) ->
      tmp = @executions().map (item) ->
        item.id
      idx = tmp.indexOf(event.executionId)
      if (idx >= 0)
        console.log "Updating execution: " + event.executionId
        @executions()[idx] = event
      else
        console.log "Adding new execution: " + event.executionId
        @executions.push event

    processWsMessage: (data) ->
      if (data.event && data.event == "execution")
        @executionEvent(data)
      undefined