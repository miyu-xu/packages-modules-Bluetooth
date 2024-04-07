#!/bin/bash
# Generates Debian source and binary packages of aconfig.

if [ -z "$1" ]; then
        echo "Usage: gen-src-pkg.sh <output-dir>"
        exit 1
fi

outdir="$1"
pkgdir=aconfig-240403
origtar=aconfig_240403.orig.tar.gz
scriptdir="$( cd "$( dirname "$0" )" && pwd )"

# Pin the branch + commit
build_branch=main
build_commit=ddfd2f48895512149d43e35b79368c54cef16081

tmpdir=$(mktemp -d)
echo Generating source package in "${tmpdir}".

# Download android/platform/build source.
cd "${tmpdir}"
git clone --branch "${build_branch}" https://android.googlesource.com/platform/build || exit 1
(cd build && git checkout "${build_commit}")

# Create package folder. Only aconfig/ is needed.
mkdir "${pkgdir}"
cd "${pkgdir}"
cp -r ../build/tools/aconfig/* .

# Apply CHROMIUM patches to aconfig.
for patch in "${scriptdir}"/debian/patches/*.patch; do
  patch -p3 < "${patch}"
done

# Clean up temporary checkout.
cd ..
rm -rf build

# Create source tarball.
tar czf "${origtar}" "${pkgdir}"

# Build debian binary package.
cd "${tmpdir}/${pkgdir}/aconfig"
cargo deb || exit 1

# Copy the results to output dir.
cd "${tmpdir}"
mkdir -p "${outdir}/src"
cp *.orig.tar.gz "${outdir}/src"
cp "${tmpdir}/${pkgdir}"/target/debian/*.deb "${outdir}"
cd /

echo Removing temporary directory "${tmpdir}".
rm -rf "${tmpdir}"

echo Done. Check out Debian source package in "${outdir}".
