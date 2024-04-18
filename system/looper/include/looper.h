
#pragma once
#include <functional>

//#include <base/functional/bind.h>
//#include <base/location.h>

class ILooper {
public:
   virtual ~ILooper() {};
   virtual void post(std::function<void()> &&f) = 0;
};

