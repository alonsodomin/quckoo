define ['knockout', 'jquery'], (ko, $) ->
  class ExecutionsView
    constructor: () ->
      @baseUri = '/executions'
      @socketUrl = jsRoutes.controllers.ExecutionController.executionsWs().webSocketURL()
      @executions = ko.observableArray()
      @socket = new WebSocket(@socketUrl)
      @socket.onmessage = (msg) =>
        @processWsMessage msg.data
      @initialize()

    initialize: () ->
      $.get @baseUri + '/history', (result) =>
        $.each result, (idx, item) =>
          @executions.push item

    executionEvent: (event) ->
      tmp = @executions.map (item) ->
        item.id
      idx = tmp.indexOf(event.executionId)
      if (idx >= 0)
        console.log "Updating execution: " + event.executionId
        @executions[idx] = ko.observable(event)
      else
        console.log "Adding new execution: " + event.executionId
        @executions.push
          id: event.executionId
          status: event.status

    processWsMessage: (data) =>
      console.log "Received: " + JSON.stringify data
      console.log data["event"]
      if (data.event && data.event == "execution")
        @executionEvent(data)
      undefined