define ['knockout', 'jquery'], (ko, $) ->
  class ExecutionsView
    constructor: () ->
      @baseUri = '/executions'
      @executions = ko.observableArray()
      @initialize()

    initialize: () ->
      $.get @baseUri + '/history', (result) =>
        $.each result, (idx, item) =>
          @executions.push item