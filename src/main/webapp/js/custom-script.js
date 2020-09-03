function drawCpuChart(root, data) {
    // Remove the first entry (header)
    data.shift();

    // Build a list of labels
    var labels = data.map(function(x) {
        return x[0];
    });
    // Build a list of values
    var values = data.map(function(x) {
        return x[1];
    });

    // We actually only want the total/used values for now, so reduce the values to them
    var total = values.reduce((a, b) => a + b);
    var used = values.length > 1 ? values.slice(0,-1).reduce((a, b) => a + b) : 0;

    // Build the config and draw the chart
    var config = buildPieConfig('CPU Usage', used, total);
    var ctx = root.getElementById('cpu-usage-chart').getContext('2d');
    var myPie = new Chart(ctx, config);
}

function drawMemoryChart(root, data) {
    // Remove the first entry (header)
    data.shift();

    // Build a list of labels
    var labels = data.map(function(x) {
        return x[0];
    });
    // Build a list of values
    var values = data.map(function(x) {
        return x[1];
    });

    // We actually only want the total/used values for now, so reduce the values to them
    var total = values.reduce((a, b) => a + b);
    var used = values.length > 1 ? values.slice(0,-1).reduce((a, b) => a + b) : 0;

    // Build the config and draw the chart
    var config = buildPieConfig('Memory Usage', used, total);
    var ctx = root.getElementById('mem-usage-chart').getContext('2d');
    var myPie = new Chart(ctx, config);
}

function drawNodeCpuChart(root, nodeName, data) {

}

function drawNodeMemoryChart(root, nodeName, data) {
    // Remove the first entry (header)
    data.shift();

    // Build a list of labels
    var labels = data.map(function(x) {
        return x[0];
    });
    // Build a list of values
    var values = data.map(function(x) {
        return x[1];
    });

    // Build the config and draw the chart
    var config = {
        type: 'pie',
        data: {
            datasets: [{
                data: values,
                backgroundColor: ['red', 'green'],
                borderWidth: 1
            }],
            labels: labels
        },
        options: {
            responsive: false,
            maintainAspectRatio: false,
            title: {
                display: false,
                text: 'memory',
                padding: 0
            },
            legend: {
                display: false,
                labels: {
                    usePointStyle: true,
                },
            },
            tooltips: {
                enabled: false,
            },
            plugins: {
                datalabels: {
                    backgroundColor: function(context) {
                        return context.dataset.backgroundColor;
                    },
                    color: 'white',
                    padding: 2,
                    display: false,
                    font: {
                        weight: 'bold'
                    },
                    formatter: (value, ctx) => {
                        let sum = 0;
                        let dataArr = ctx.chart.data.datasets[0].data;
                        dataArr.map(data => {
                            sum += data;
                        });
                        let percentage = (value*100 / sum).toFixed(1) + '%';
                        return percentage;
                    }
                }
            }
        }
    };
    var ctx = root.getElementById('memory-' + nodeName).getContext('2d');
    var myPie = new Chart(ctx, config);
}

function buildPieConfig(title, usedValue, totalValue) {
    var config = {
        type: 'pie',
        data: {
            datasets: [{
                data: [usedValue, totalValue],
                backgroundColor: ['red', 'green'],
                borderWidth: 1,
                datalabels: {
                    anchor: 'center'
                }
            }],
            labels: ['Used', 'Free']
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            title: {
                display: true,
                text: title,
                padding: 0
            },
            legend: {
                display: true,
                labels: {
                    usePointStyle: true,
                },
            },
            tooltips: {
                enabled: true,
            },
            plugins: {
                datalabels: {
                    backgroundColor: function(context) {
                        return context.dataset.backgroundColor;
                    },
                    color: 'white',
                    padding: 2,
                    display: true,
                    font: {
                        weight: 'bold'
                    },
                    formatter: (value, ctx) => {
                        let sum = 0;
                        let dataArr = ctx.chart.data.datasets[0].data;
                        dataArr.map(data => {
                            sum += data;
                        });
                        let percentage = (value*100 / sum).toFixed(1) + '%';
                        return percentage;
                    }
                }
            }
        }
    };
    return config;
}
