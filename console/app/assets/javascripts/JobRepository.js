define(['knockout', 'jquery'], function (ko, $) {
    return function JobRepositoryView() {
        var self = this;

        self.baseUri = "/repository";
        self.jobs = ko.observableArray([]);

        self._initialize = function() {
            $.get(self.baseUri + '/jobs', function (result) {
                $.each(result, function (idx, item) {
                    self.jobs.push(item);
                });
            });
        };

        self._initialize();
    };
});