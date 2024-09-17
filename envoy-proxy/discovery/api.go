package discovery

import "context"

type ServiceDiscovery interface {
	listEndpoints(ctx context.Context, serviceName, env string) []Endpoint
	SelectEndpoint(ctx context.Context, serviceName, env string) (Endpoint, bool)
}
