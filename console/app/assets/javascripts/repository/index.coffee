require ['knockout', 'JobRepositoryView', 'requirejs-domready!'], (ko, JobRepositoryView) ->
  ko.applyBindings(new JobRepositoryView())