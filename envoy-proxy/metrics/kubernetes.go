package metrics

import (
	"context"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"log/slog"
)

func AutoDiscoverMetrics() (Client, error) {
	config, err := rest.InClusterConfig()
	if err != nil {
		slog.Error("Failed to create Kubernetes client config", "err", err.Error())
		return NewDummyMetricsClient(), err
	}

	clientSet, err := kubernetes.NewForConfig(config)
	if err != nil {
		slog.Error("Failed to create Kubernetes client", "err", err.Error())
		return NewDummyMetricsClient(), err
	}

	namespaces, err := clientSet.CoreV1().Namespaces().List(context.Background(), metav1.ListOptions{})
	if err != nil {
		return NewDummyMetricsClient(), err
	}

	for _, ns := range namespaces.Items {

		// search for dagStatsD service
		services, err := clientSet.CoreV1().Services(ns.Name).List(context.Background(), metav1.ListOptions{
			FieldSelector: "metadata.name=datadog-agent",
		})
		if err != nil {
			slog.Error("Failed to list services using k8s api when auto discover datadog agent", "err", err.Error())
			return NewDummyMetricsClient(), err
		}
		if len(services.Items) > 0 {
			service := services.Items[0]
			host := service.Spec.ClusterIP
			port := 8125
			if len(service.Spec.Ports) > 0 {
				port = int(service.Spec.Ports[0].Port)
			}
			return NewDogStatsDClient(host, port)
		}

	}

	return NewDummyMetricsClient(), err
}
