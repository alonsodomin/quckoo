define ['knockout'], (ko) ->
  class ExecutionModel
    constructor: (executionId, status) ->
      @executionId = ko.observable(executionId)
      @status = ko.observable(status)