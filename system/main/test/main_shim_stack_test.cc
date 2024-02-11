/*
 *  Copyright 2024 The Android Open Source Project
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

#include <fcntl.h>
#include <gmock/gmock.h>
#include <gtest/gtest.h>
#include <unistd.h>

#include <cstdint>

#include "module_gdx_base_unittest.h"
#include "test/mock/mock_main_shim_entry.h"

using ::testing::_;

using namespace bluetooth;
using namespace testing;
using HciHandle = uint16_t;

class TestModule : public bluetooth::Module {
 public:
  TestModule(const TestModule&) = delete;
  TestModule& operator=(const TestModule&) = delete;

  virtual ~TestModule(){};
  static const ModuleFactory Factory;

  virtual std::string Name() const = 0;

  void test() const { LOG_INFO("Test base clasee"); }

  void test2(std::string s) const {
    LOG_INFO("Test2 base clasee:%s", s.c_str());
  }

 protected:
  void ListDependencies(ModuleList* list) const override{};
  void Start() override{};
  void Stop() override{};
  std::string ToString() const override { return std::string("TestFunction"); }

  TestModule() { LOG_INFO("Created test module"); }

 private:
};

class TestModule1 : public TestModule {
 public:
  TestModule1(const TestModule1&) = delete;
  TestModule1& operator=(const TestModule1&) = delete;

  virtual ~TestModule1(){};
  static const ModuleFactory Factory;

  std::string Name() const override { return std::string("TestModule1"); }
  TestModule1() : TestModule() { LOG_INFO("Created test module1"); }
  void module1(std::string s) const { LOG_INFO("CMM module1: %s", s.c_str()); }

 protected:
 private:
};

class TestModule2 : public TestModule {
 public:
  TestModule2(const TestModule2&) = delete;
  TestModule2& operator=(const TestModule2&) = delete;

  virtual ~TestModule2(){};
  static const ModuleFactory Factory;

  std::string Name() const override { return std::string("TestModule2"); }
  void module2(std::string s) const { LOG_INFO("CMM module2: %s", s.c_str()); }

 protected:
  TestModule2() : TestModule() { LOG_INFO("Created test module2"); }

 private:
};

class TestModule3 : public TestModule {
 public:
  TestModule3(const TestModule&) = delete;
  TestModule3& operator=(const TestModule3&) = delete;

  virtual ~TestModule3(){};
  static const ModuleFactory Factory;

  std::string Name() const override { return std::string("TestModule3"); }
  void module3(std::string s) const { LOG_INFO("CMM module3: %s", s.c_str()); }

 protected:
  TestModule3() : TestModule() { LOG_INFO("Created test module3"); }

 private:
};

class TestModule4 : public TestModule {
 public:
  TestModule4(const TestModule&) = delete;
  TestModule4& operator=(const TestModule3&) = delete;

  virtual ~TestModule4(){};
  static const ModuleFactory Factory;

  std::string Name() const override { return std::string("TestModule4"); }
  void module4(std::string s) const { LOG_INFO("CMM module4: %s", s.c_str()); }

 protected:
  TestModule4() : TestModule() { LOG_INFO("Created test module4"); }

 private:
};

const ModuleFactory TestModule1::Factory =
    ModuleFactory([]() { return new TestModule1(); });
const ModuleFactory TestModule2::Factory =
    ModuleFactory([]() { return new TestModule2(); });
const ModuleFactory TestModule3::Factory =
    ModuleFactory([]() { return new TestModule3(); });
const ModuleFactory TestModule4::Factory =
    ModuleFactory([]() { return new TestModule4(); });

class ModuleUnitTest : public ModuleStackUnitTest {
 protected:
  void SetUp() override {
    ModuleStackUnitTest::SetUp();
    AddModule<TestModule1>();
    AddModule<TestModule2>();
    AddModule<TestModule3>();
    StartStack();
  }

  void TearDown() override { ModuleStackUnitTest::TearDown(); }
};

TEST_F(ModuleUnitTest, NOP) {}

#if 0
TEST_F(ModuleUnitTest, NOP) {
  LOG_INFO("CMM About to start test");
  ASSERT_STREQ("TestModule1", bluetooth::shim::Stack::GetInstance()->GetStackManager()->GetInstance<TestModule1>()->Name().c_str());
  ASSERT_STREQ("TestModule2", bluetooth::shim::Stack::GetInstance()->GetStackManager()->GetInstance<TestModule2>()->Name().c_str());
  ASSERT_STREQ("TestModule3", bluetooth::shim::Stack::GetInstance()->GetStackManager()->GetInstance<TestModule3>()->Name().c_str());
//  auto module = bluetooth::shim::Stack::GetSafeModule<TestModule4>();
//  if (module) {
//    ASSERT_STREQ("TestModule4", module->Name().c_str());
//  } else {
//    LOG_INFO("No module4 exists");
//  }
}
#endif

#if 0
TEST_F(ModuleUnitTest, NOP3) {
  // TestModule1 mod;
  // auto f1 = std::bind(bluetooth::shim::Stack::GetModule<TestModule1>(), ::module1);
  auto f3 = std::bind(&TestModule1::module1, bluetooth::shim::Stack::GetModule<TestModule1>(), std::placeholders::_1);
  LOG_INFO("About to run the base bind operation");
  f3("hello world");
  LOG_INFO("Ran the base bind operation");
//  bluetooth::shim::Stack::GetSafeModule5<TestModule1>()->module1();
}

TEST_F(ModuleUnitTest, NOP4) {
  auto greet = std::mem_fn(&TestModule1::module1);
  // greet(bluetooth::shim::Stack::GetModule<TestModule1>(), "one");
  auto mod = bluetooth::shim::Stack::GetSafeModule<TestModule1>();
  if (mod) {
    greet(mod, "one");
  } else {
    LOG_INFO("Module unavailable");
  }

  auto greet2 = std::mem_fn(&TestModule2::module2);
  auto mod2 = bluetooth::shim::Stack::GetSafeModule<TestModule2>();
  if (mod2) {
    greet2(mod2, "Call from NOP4");
  } else {
    LOG_INFO("Module unavailable");
  }

  {
    auto greet3 = std::mem_fn(&TestModule3::module3);
    auto mod3 = bluetooth::shim::Stack::GetSafeModule<TestModule3>();
    if (mod3) {
      greet3(mod3, "Another parameter");
    } else {
      LOG_INFO("Module unavailable");
    }
  }

  auto greet4 = std::mem_fn(&TestModule4::module4);
  auto mod4 = bluetooth::shim::Stack::GetSafeModule<TestModule4>();
  if (mod4) {
    greet4(mod4, "Should not hit");
  } else {
    LOG_INFO("Module unavailable");
  }
}

TEST_F(ModuleUnitTest, NOP5) {
  bluetooth::shim::Stack::CallOnModule5<TestModule1>([](TestModule1* mod){
    LOG_INFO("Called within lambda");
    mod->test();
    mod->module1("Hello from module 1");
  });
  bluetooth::shim::Stack::CallOnModule5<TestModule2>([](TestModule2* mod){
    LOG_INFO("Called within lambda");
    mod->test();
    mod->module2("Hello from module 2");
  });
  bluetooth::shim::Stack::CallOnModule5<TestModule3>([](TestModule3* mod){
    LOG_INFO("Called within lambda");
    mod->test();
    mod->module3("Hello from module 3");
  });
  bluetooth::shim::Stack::CallOnModule5<TestModule4>([](TestModule4* mod){
    LOG_INFO("Called within lambda");
    mod->test();
    mod->module4("Hello");
  });
  LOG_INFO("NOP5");
//  if (mod) {
//    greet(mod, "one");
//  } else {
//    LOG_INFO("Module unavailable");
//  }
}

TEST_F(ModuleUnitTest, NOP6) {
  auto f = std::bind(&TestModule1::module1, bluetooth::shim::Stack::GetSafeModule<TestModule1>(), "test_from_bind");
  f();

  auto f4 = std::bind(&TestModule4::module4, bluetooth::shim::Stack::GetSafeModule<TestModule4>(), "test_from_bind4");
  f4();
}
#endif
