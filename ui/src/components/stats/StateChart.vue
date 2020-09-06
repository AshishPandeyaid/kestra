<template>
    <div :id="uuid" :class="'executions-charts' + (this.global ? '' : ' mini')" v-if="dataReady">
        <current-chart :data="collections" :options="options"></current-chart>
        <b-tooltip
            custom-class="tooltip-stats"
            no-fade
            :target="uuid"
            :placement="(this.global ? 'bottom' : 'left')"
            triggers="hover">
            <span v-html="tooltip"></span>
        </b-tooltip>
    </div>
</template>

<script>
    import {Bar} from 'vue-chartjs'
    import {uid} from "../../utils/utils.js";
    import {tooltip, defaultConfig} from "../../utils/charts.js";

    const CurrentChart = {
        extends: Bar,
        props: {
            data: {
                type: Object,
                required: true
            },
            options: {
                type: Object,
                required: true
            }
        },
        mounted() {
            setTimeout(() => {
                this.renderChart(this.data, this.options);
            }, 0)
        },
    };

    export default {
        components: {
            CurrentChart
        },
        props: {
            data: {
                type: Array,
                required: true
            },
            global: {
                type: Boolean,
                default: () => false
            }
        },
        data() {
            return {
                uuid: uid(),
                tooltip: undefined
            };
        },
        methods: {
            backgroundFromState(state) {
                if (state === "SUCCESS") {
                    return "#43ac6a"
                } else if (state === "CREATED") {
                    return "#75bcdd"
                } else if (state === "RUNNING") {
                    return "#1AA5DE"
                } else {
                    return "#F04124"
                }
            }
        },
        computed: {
            dataReady() {
                return this.data.length > 0;
            },
            collections() {
                let self = this;

                let datasets = this.data
                    .reduce(function (accumulator, value) {
                        Object.keys(value.executionCounts).forEach(function (state) {
                            if (accumulator[state] === undefined) {
                                accumulator[state] = {
                                    label: state,
                                    backgroundColor: self.backgroundFromState(state),
                                    data: []
                                };
                            }

                            accumulator[state].data.push(value.executionCounts[state]);
                        });

                        return accumulator;
                    }, Object.create(null))


                return {
                    labels: this.data.map(r => r.startDate),
                    datasets: Object.values(datasets)
                }
            },
            options() {
                let self = this

                return defaultConfig({
                    tooltips: {
                        custom: function (tooltipModel) {
                            let content = tooltip(tooltipModel);
                            if (content) {
                                self.tooltip = content;
                            }
                        }
                    },

                    scales: {
                        xAxes: [{
                            stacked: true,
                        }],
                        yAxes: [{
                            stacked: true,
                        }]
                    },
                })
            }
        }
    }
</script>