<template>
    <b-card no-body>
        <b-tabs card>
            <b-tab
                v-for="tab in tabs"
                :key="tab.tab"
                @click="setTab(tab.tab)"
                :active="$route.query.tab === tab.tab"
                :title="tab.title"
            >
                <b-card-text>
                    <div :is="tab.tab" />
                </b-card-text>
            </b-tab>
        </b-tabs>
    </b-card>
</template>
<script>
import Gantt from "./Gantt";
import Overview from "./Overview";
import Logs from "./Logs";
import Topology from "./Topology";
import Trigger from "vue-material-design-icons/Cogs";
import BottomLine from "../layout/BottomLine";
import FlowActions from "../flows/FlowActions";
import RouteContext from "../../mixins/routeContext";

export default {
    mixins: [RouteContext],
    components: {
        Overview,
        BottomLine,
        Trigger,
        Gantt,
        Logs,
        Topology,
        FlowActions
    },
    data() {
        return {
            sse: undefined
        };
    },
    created() {
        this.$store.dispatch("execution/loadExecution", this.$route.params);
        this.$store
            .dispatch("execution/followExecution", this.$route.params)
            .then(sse => {
                this.sse = sse;
                sse.subscribe("", data => {
                    this.$store.commit("execution/setExecution", data);
                });
            });
    },
    methods: {
        setTab(tab) {
            this.$store.commit("execution/setTask", undefined);
            this.$router.push({
                name: "execution",
                params: this.$route.params,
                query: { tab }
            });
        }
    },
    computed: {
        routeInfo() {
            return {
                title: this.$t("execution"),
                breadcrumb: [
                    {
                        label: this.$t("flows"),
                        link: {
                            name: "flowsList",
                            query: {
                                namespace: this.$route.params.namespace
                            }
                        }
                    },
                    {
                        label: this.$route.params.namespace + "." + this.$route.params.flowId,
                        link: {
                            name: "flow",
                            params: {
                                namespace: this.$route.params.namespace,
                                id: this.$route.params.flowId
                            }
                        }
                    },
                    {
                        label: this.$t("executions"),
                        link: {}
                    },
                    {
                        label: this.$route.params.id,
                        link: {
                            name: "execution"
                        }
                    }
                ]
            };
        },
        tabs() {
            const title = title => this.$t(title).capitalize();
            return [
                {
                    tab: "overview",
                    title: title("overview")
                },
                {
                    tab: "gantt",
                    title: title("gantt")
                },
                {
                    tab: "logs",
                    title: title("logs")
                },
                {
                    tab: "topology",
                    title: title("topology")
                }
            ];
        }
    },
    beforeDestroy() {
        this.sse.close();
    }
};
</script>