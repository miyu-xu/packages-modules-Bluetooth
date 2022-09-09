from glob import glob
import re
from collections import defaultdict

FILES = [
    *glob("system/**/*.*", recursive=True),
]

CORE_FILE_KEYWORDS = [
    "_dm",
    "/sdp/",
    "sec",
    "btcore",
    "gatt",
    "/l2cap/",
    "l2c_",
    "sock_thread",
    "sock_util",
    "stack_manager",
    "system/common",
    "/gd/",
    "system/main",
    "/osi/" "/packet/",
    "stack/btm",
    "stack/gap",
    "smp",
    "/acl",
    "/btu/",
    "crypto_toolbox",
    "bluetooth.cc",
    "bta/sys/",
    "btif_sdp.cc",
]

NAMESPACE = re.compile(r"\s*namespace\s([a-zA-Z0-9]+)?\s?{")
CLASS = re.compile(r"\s*(?:class|struct) ([a-zA-Z0-9_:]+).*{")
DEFN = re.compile(
    r"^(?:\s|extern|static|inline|const|virtual)*([a-zA-Z0-9_*<>:&]+) ([a-zA-Z0-9_:&]+)\(",
    re.MULTILINE,
)
CALL = re.compile(r"([a-zA-Z0-9_:&]+)", re.MULTILINE)

declarations = defaultdict(list)
definitions = defaultdict(list)
declarations_by_call_name = defaultdict(list)
definitions_by_call_name = defaultdict(list)

static_cnt = 0


def is_valid_file(file):
    # if "gd" in file:
    #   return False
    if "hal_interface" in file:
        # skip weird AIDL stuff
        return False
    if "_linux" in file:
        # skip weird linux alternatives
        return False
    if "codec" in file:
        return False
    if "test" in file or "mock" in file or "benchmark" in file:
        return False
    if not (file.endswith(".cc") or file.endswith(".h")):
        # only look at C++ (ish)
        return False
    if "shim" in file:
      return False
    return True


def is_core_file(file):
    return (
        any(keyword in file for keyword in CORE_FILE_KEYWORDS) and "topshim" not in file
    )


for file in FILES:
    if not is_valid_file(file):
        continue

    with open(file) as f:
        brace_cnt = 0
        namespace_stack = []

        found = False
        for line in f:
            line = re.split("//", line, maxsplit=1)[0]
            while line.strip().endswith(",") or line.strip().endswith(">"):
                curr = next(f)
                curr = re.split("//", curr, maxsplit=1)[0]
                line += " " + curr

            if m := NAMESPACE.match(line):
                name = m.group(1)
                namespace_stack.append([brace_cnt, name or ""])
                brace_cnt += 1
                continue

            if m := CLASS.match(line):
                name = m.group(1)
                namespace_stack.append([brace_cnt, name or ""])
                brace_cnt += 1
                continue

            brace_cnt += line.count("{")
            brace_cnt -= line.count("}")

            while namespace_stack and namespace_stack[-1][0] >= brace_cnt:
                namespace_stack.pop()

            names = [x[1] for x in namespace_stack]

            file_defns = set(DEFN.findall(line))

            for rettype, defn in file_defns:
                # print("> " + defn, rettype, namespace_stack, brace_cnt)
                # if None in names or "" in names:
                #   # anonymous, skip
                #   continue

                if defn in {"main", "init"}:
                    continue

                full_defn = "::".join(names) + "::" + defn if names else defn
                defn = defn.split(":")[-1]

                if rettype in {"return", "<<"}:
                    continue

                # if "static" in line:
                #   static_cnt += 1
                #   continue

                found = True
                # print("> " + full_defn)

                if file.endswith(".h"): # or line.strip().endswith(";"):
                    declarations[full_defn].append(file)
                    declarations_by_call_name[defn].append(file)
                else:
                    # if full_defn in definitions and file != definitions[full_defn]:
                    #   raise Exception(f"{full_defn} defined in two places: {definitions[full_defn]} and {file}: {repr(line)}")
                    definitions[full_defn].append(file)
                    definitions_by_call_name[defn].append(file)

        # if not found and ".cc" in file:
        #     print(file)

usages = defaultdict(set)

unknowns = set()

for file in FILES:
    if not is_valid_file(file):
        continue
    if "/include/" in file:
        # includes are weird, skip them for usage analysis
        continue
    with open(file) as f:
        for call in CALL.findall(f.read()):
            call = call.split(":")[-1]
            if call in definitions_by_call_name or call in declarations_by_call_name:
                usages[call].add(file)
            elif call == call.upper() or call.endswith("_"):
                pass
            else:
                unknowns.add(call)


cnt = 0

core_api = defaultdict(list)

for usage, where in usages.items():
    if all(is_core_file(file) for file in where):
        # if only used in core, skip
        continue

    non_core = [file for file in where if not is_core_file(file)]

    for decl in declarations_by_call_name[usage]:
        if is_core_file(decl):
            core_api[usage] = non_core
            break
    else:
        for decl in definitions_by_call_name[usage]:
            if is_core_file(decl):
                core_api[usage] = non_core
                break

        #   # looks like an out-of-core -> core dep
        #   print(f"Function {usage} declared in {decl} and used in non_core: {next(iter(non_core))}")
        #   cnt += 1
        #   break

for api in sorted(core_api):
    # , key=lambda x: len(core_api[x]), reverse=True):
    if len(definitions_by_call_name[api]) > 4:
        continue
    # print(api, next(decl for decl in definitions_by_call_name[api] if is_core_file(decl)), len(core_api[api]), len(definitions_by_call_name[api]))

print(
    cnt, len(core_api), len(unknowns), len(definitions_by_call_name), len(declarations)
)

core_api_headers = sorted(
    {
        api + " " + decl
        for api in core_api
        for decl in (declarations_by_call_name[api] + definitions_by_call_name[api])
        if is_core_file(decl)
    }
)

def categorize(x):
  if "shim" in x:
    return "Shim"
  if "gatt" in x:
    return "GATT"
  if "l2ca" in x:
    return "L2CAP"
  if "acl" in x:
    return "ACL"
  if "sco" in x:
    return "SCO"

print("\n".join(map(str, core_api_headers)))
