#include <limits.h>
#include <bitset>
#include <ctime>

#include "gtest/gtest.h"

#include "Types.h"
#include "Utils.h"
#include "SynSearch.h"

#define ALL_CATS_HAVE_FURRY_TAILS \
                       string(ALL_STR) + string("\t2\tdet\t0\tanti-additive\t2-3\tmultiplicative\t3-5\n") + \
                       string(CAT_STR) + string("\t3\tnsubj\t0\t-\t-\t-\t-\n") + \
                       string(HAVE_STR) + string("\t0\troot\t0\t-\t-\t-\t-\n") + \
                       string(FURRY_STR) + string("\t5\tamod\t0\t-\t-\t-\t-\n") + \
                       string(TAIL_STR) + string("\t3\tdobj\t0\t-\t-\t-\t-\n")

using namespace std;

class UtilsTest : public ::testing::Test {
 protected:
  virtual void SetUp() {
    graph = ReadMockGraph();
    ASSERT_FALSE(graph == NULL);
    tree = new Tree(ALL_CATS_HAVE_FURRY_TAILS);
  }

  virtual void TearDown() {
    delete graph;
  }

  Graph* graph;
  Tree* tree;
};

TEST_F(UtilsTest, pathToStringTest) {
  vector<SearchNode> path;
  uint32_t i = 0;

  // Construct path
  // (root -- all furry cats have tails)
  const SearchNode root(*tree);
  path.insert(path.begin(), root);
  EXPECT_EQ("all cat have furry tail", kbGloss(*graph, *tree, path));
  // (move to cats)
  const SearchNode allCATSHaveTails(root, *tree, ++i, 1);
  path.insert(path.begin(), allCATSHaveTails);
  EXPECT_EQ("all cat have furry tail", kbGloss(*graph, *tree, path));
  // (mutate cats -> animal)
  edge e;
  e.source = ANIMAL.word; e.source_sense = ANIMAL.sense;
  e.sink   = CAT.word;    e.sink_sense   = CAT.sense;
  e.type = HYPERNYM; e.cost = 0.1f;
  const SearchNode allANIMALSHaveTails
    = allCATSHaveTails.mutation(e, ++i, true, *tree, graph);
  path.insert(path.begin(), allANIMALSHaveTails);
  EXPECT_EQ("all animal have furry tail", kbGloss(*graph, *tree, path));
  // (move to tails)
  const SearchNode allAnimalsHaveTAILS(allANIMALSHaveTails, *tree, 4, ++i);
  path.insert(path.begin(), allAnimalsHaveTAILS);
  EXPECT_EQ("all animal have furry tail", kbGloss(*graph, *tree, path));
  // (mutate tail -> lemur)
  e.source = LEMUR.word; e.source_sense = LEMUR.sense;
  e.sink   = TAIL.word;    e.sink_sense   = TAIL.sense;
  e.type = ANGLE_NN; e.cost = 1.0f;
  const SearchNode allAnimalsHaveLEMUR
    = allAnimalsHaveTAILS.mutation(e, ++i, true, *tree, graph);
  path.insert(path.begin(), allAnimalsHaveLEMUR);
  EXPECT_EQ("all animal have furry lemur", kbGloss(*graph, *tree, path));
  // (move to furry)
  const SearchNode allAnimalsHaveFURRYLemur(allAnimalsHaveLEMUR, *tree, 3, ++i);
  path.insert(path.begin(), allAnimalsHaveFURRYLemur);
  EXPECT_EQ("all animal have furry lemur", kbGloss(*graph, *tree, path));
  // (delete furry)
  const SearchNode sink = allAnimalsHaveFURRYLemur.deletion(++i, true, *tree, 3);
  path.insert(path.begin(), sink);
  EXPECT_EQ("all animal have lemur", kbGloss(*graph, *tree, path));
}


TEST_F(UtilsTest, ToStringTree) {
  Tree tree(CAT_STR + string("\t2\tnsubj\n") +
            HAVE_STR + string("\t0\troot\n") +
            TAIL_STR + string("\t2\tdobj"));
  EXPECT_EQ(string("[^]cat_0 [^]have_0 [^]tail_0"), toString(*graph, tree));
}

TEST_F(UtilsTest, ToStringTime) {
  EXPECT_EQ(string("0s"), toString(0l * CLOCKS_PER_SEC));
  EXPECT_EQ(string("1s"), toString(1l * CLOCKS_PER_SEC));
  EXPECT_EQ(string("59s"), toString(59l * CLOCKS_PER_SEC));
  EXPECT_EQ(string("1m 0s"), toString(60l * CLOCKS_PER_SEC));
  EXPECT_EQ(string("1m 1s"), toString(61l * CLOCKS_PER_SEC));
  EXPECT_EQ(string("2h 20m 3s"), toString((2l*60l*60l + 20l*60l + 3l) * CLOCKS_PER_SEC));
  EXPECT_EQ(string("10d 2h 20m 3s"), toString((10l*24l*60l*60l + 2l*60l*60l + 20l*60l + 3l) * CLOCKS_PER_SEC));
}

TEST_F(UtilsTest, FastATOI) {
  EXPECT_EQ(0, fast_atoi("0"));
  EXPECT_EQ(1, fast_atoi("1"));
  EXPECT_EQ(104235, fast_atoi("104235"));
  for (uint8_t shift = 0; shift < 31; ++shift) {
    uint32_t i = (1l << shift);
    char buffer[32];
    snprintf(buffer, 31, "%u", i);
    EXPECT_EQ(i, fast_atoi(buffer));
    EXPECT_EQ(atoi(buffer), fast_atoi(buffer));
  }
}

TEST_F(UtilsTest, FastATOIPeculiarities) {
  // Parsing from PSQL is a bit of a pain, and it makes it easier
  // if we can just accept the final '}' and discard it in atoi()
  EXPECT_EQ(104235, fast_atoi("104235}"));
}


