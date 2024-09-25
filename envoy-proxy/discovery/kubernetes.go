package discovery

import (
	"context"
	"fmt"
	v1 "k8s.io/api/core/v1"
	metav1 "k8s.io/apimachinery/pkg/apis/meta/v1"
	"k8s.io/client-go/informers"
	"k8s.io/client-go/kubernetes"
	"k8s.io/client-go/rest"
	"k8s.io/client-go/tools/cache"
	"log/slog"
	"os"
	"os/signal"
	"syscall"
	"time"
)

type K8sServiceDiscovery struct {
	BaseServiceDiscovery
	clientSet *kubernetes.Clientset
}

func NewK8sServiceDiscovery() *K8sServiceDiscovery {
	config, err := rest.InClusterConfig()
	if err != nil {
		panic("Failed to create Kubernetes client config: " + err.Error())
	}

	clientSet, err := kubernetes.NewForConfig(config)
	if err != nil {
		panic("Failed to create Kubernetes client: " + err.Error())
	}

	return &K8sServiceDiscovery{
		BaseServiceDiscovery: NewBaseServiceDiscovery(),
		clientSet:            clientSet,
	}
}

func (sd *K8sServiceDiscovery) listEndpoints(ctx context.Context, serviceName, env string) []Endpoint {

	// Get list of pods for the deployment
	pods, err := sd.clientSet.CoreV1().Pods(env).List(ctx, metav1.ListOptions{
		LabelSelector: fmt.Sprintf("%s=%s", RpcServiceLabel, serviceName),
	})
	if err != nil {
		slog.Error("Failed to list pods using k8s api", "err", err.Error())
		return []Endpoint{}
	}

	// Loop through the pods and get IP addresses of running pods
	for _, pod := range pods.Items {
		if isPodReady(&pod) {
			if ep, ok := getEndpointFromPodSpec(&pod); ok {
				slog.Info("Adding pod Endpoint", "name", pod.Name, "host", ep.Addr, "port", ep.Port, "service", serviceName, "env", env)
				sd.BaseServiceDiscovery.addEndpoint(ctx, serviceName, env, ep)
			}
		}
	}
	return sd.BaseServiceDiscovery.listEndpoints(ctx, serviceName, env)
}

func (sd *K8sServiceDiscovery) SelectEndpoint(ctx context.Context, serviceName, env string) (Endpoint, bool) {
	key := getEndpointGroupKey(serviceName, env)
	if eg, ok := sd.serviceMap[key]; !ok {
		slog.Debug("Service not found in cache, listing endpoints", "service", serviceName, "env", env)
		endpoints := sd.listEndpoints(ctx, serviceName, env)
		if len(endpoints) == 0 {
			return Endpoint{}, false
		} else {
			return endpoints[0], true
		}
	} else {
		return eg.next()
	}
}

func (sd *K8sServiceDiscovery) Watch(ctx context.Context) {

	sigCh := make(chan os.Signal)
	stopCh := make(chan struct{})
	signal.Notify(sigCh, syscall.SIGTERM, syscall.SIGINT)
	// close the stopCh when the system exits
	go func() {
		defer close(stopCh)
		<-sigCh
	}()

	factory := informers.NewSharedInformerFactory(sd.clientSet, 30*time.Second)
	podInformer := factory.Core().V1().Pods().Informer()

	_, err := podInformer.AddEventHandler(cache.ResourceEventHandlerFuncs{
		UpdateFunc: func(oldObj, newObj interface{}) {
			defer func() {
				if r := recover(); r != nil {
					slog.Error("Discovery recovered from panic", "error", r)
				}
			}()

			oldState := oldObj.(*v1.Pod)
			newState := newObj.(*v1.Pod)

			var serviceName, env string
			if isRpc, ok := newState.Labels[RpcLabel]; !ok || isRpc != "true" {
				return
			}
			if serviceName = newState.Labels[RpcServiceLabel]; serviceName == "" {
				return
			}
			env = newState.Namespace

			if !isPodReady(oldState) && isPodReady(newState) {
				if ep, ok := getEndpointFromPodSpec(newState); ok {
					slog.Info("Adding pod Endpoint",
						"name", newState.Name, "host", ep.Addr, "port", ep.Port, "service", serviceName, "env", env)
					sd.BaseServiceDiscovery.addEndpoint(ctx, serviceName, env, ep)
				}
			}

			if !isPodTerminating(oldState) && isPodTerminating(newState) {
				if ep, ok := getEndpointFromPodSpec(newState); ok {
					slog.Info("Deleting pod Endpoint",
						"name", newState.Name, "host", ep.Addr, "port", ep.Port, "service", serviceName, "env", env)
					sd.BaseServiceDiscovery.removeEndpoint(ctx, serviceName, env, ep)
				}
			}
		},
	})
	if err != nil {
		slog.Error("Failed to add event handler to Pod informer", err, err.Error())
		return
	}

	factory.Start(stopCh)
	factory.WaitForCacheSync(stopCh)
}

func isPodReady(pod *v1.Pod) bool {
	for _, condition := range pod.Status.Conditions {
		if condition.Type == v1.PodReady && condition.Status == v1.ConditionTrue {
			return true
		}
	}
	return false
}

func isPodTerminating(pod *v1.Pod) bool {
	return pod.DeletionTimestamp != nil
}

func getEndpointFromPodSpec(pod *v1.Pod) (Endpoint, bool) {
	for _, container := range pod.Spec.Containers {
		for _, port := range container.Ports {
			return Endpoint{
				Addr: pod.Status.PodIP,
				Port: port.ContainerPort,
			}, true
		}
	}
	return Endpoint{}, false
}
