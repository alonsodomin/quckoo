require(['knockout', 'JobRepository', 'requirejs-domready!'], function (ko, JobRepositoryView) {
    var view = new JobRepositoryView();
    ko.applyBindings(view);
});