define ['knockout', 'jquery'], (ko, $) ->
  class JobRepository
    constructor: () ->
      @baseUri = "/repository"
      @jobs = ko.observableArray([])
      @initialize()

    initialize: () ->
      $.get @baseUri + '/jobs', (result) =>
        $.each result, (idx, item) =>
          @jobs.push item
