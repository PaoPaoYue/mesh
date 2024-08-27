syntax = "proto3";

package rpc;

import "base.proto";

option java_package = "${info.rootPackage}.proto";
option java_outer_classname = "${info.serviceClass}Proto";

// ############################
// should define your proto objects here:
// message EchoRequest {
//   string text = 1;
// }
//
// message EchoResponse {
//   string text = 1;
//
//   RespBase base = 255; // should always include this field in response object
// }

// ############################

enum ServiceStatusCode {
  SERVICE_OK = 0;

// ############################
// should define your own service status code at >= 100000:
// SERVICE_ERROR = 100001;

// ############################
}

service ${info.serviceClass} {
// ############################
// should define your rpc calls here:
// rpc echo(EchoRequest) returns (EchoResponse);

// ############################
}