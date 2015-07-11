require ['knockout', 'ExecutionsView', 'requirejs-domready!'], (ko, ExecutionsView) ->
  ko.applyBindings(new ExecutionsView())